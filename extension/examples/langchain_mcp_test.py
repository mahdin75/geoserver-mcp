"""Smoke-test the GeoServer MCP extension through LangChain.

Requires a running GeoServer **2.28.x** with `gs-mcp-0.1.0.jar` installed.
Do not point this at GeoServer 2.20.x; that series has no `/mcp` endpoint.

    pip install -r extension/examples/requirements-langchain.txt
    python extension/examples/langchain_mcp_test.py

Optional: set GEOSERVER_MCP_URL, GEOSERVER_USER, GEOSERVER_PASSWORD.
Pass --agent to also run a LangChain agent through OpenRouter
(needs OPENROUTER_API_KEY, or OPENAI_API_KEY if that value is an OpenRouter key).
"""

from __future__ import annotations

import argparse
import asyncio
import base64
import json
import os
import sys
import uuid
from typing import Any


def basic_auth_header(user: str, password: str) -> dict[str, str]:
    token = base64.b64encode(f"{user}:{password}".encode("utf-8")).decode("ascii")
    return {"Authorization": f"Basic {token}"}


def _coerce_json(value: Any) -> Any:
    if isinstance(value, str):
        try:
            return json.loads(value)
        except json.JSONDecodeError:
            return value
    return value


def _unwrap_content_blocks(value: Any) -> Any:
    """LangChain MCP adapters often wrap tool JSON as [{type: text, text: "..."}]."""
    if isinstance(value, dict) and value.get("type") == "text" and "text" in value:
        return value["text"]
    if (
        isinstance(value, list)
        and value
        and all(isinstance(item, dict) and item.get("type") == "text" and "text" in item for item in value)
    ):
        return "".join(item["text"] for item in value)
    return value


def parse_tool_result(raw: Any) -> Any:
    if raw is None:
        return None
    value = _coerce_json(raw)
    value = _unwrap_content_blocks(value)
    return _coerce_json(value)


def geoserver_connection(url: str, user: str, password: str, transport: str) -> dict:
    return {
        "geoserver": {
            "transport": transport,
            "url": url,
            "headers": basic_auth_header(user, password),
        }
    }


async def load_tools(url: str, user: str, password: str, transport: str):
    from langchain_mcp_adapters.client import MultiServerMCPClient

    preferred = transport
    fallback = "http" if preferred == "streamable_http" else "streamable_http"
    last_error: Exception | None = None
    for name in (preferred, fallback):
        try:
            client = MultiServerMCPClient(geoserver_connection(url, user, password, name))
            tools = await client.get_tools()
            print(f"Connected with transport={name!r}")
            return client, tools
        except Exception as exc:
            last_error = exc
            message = str(exc).lower()
            if "transport" in message or "validation" in message:
                print(f"transport={name!r} rejected ({exc}); trying the other name")
                continue
            raise
    raise last_error  # type: ignore[misc]


async def invoke_named(tools, name: str, arguments: dict | None = None) -> Any:
    by_name = {tool.name: tool for tool in tools}
    if name not in by_name:
        raise AssertionError(f"Missing tool {name!r}. Loaded: {sorted(by_name)}")
    result = await by_name[name].ainvoke(arguments or {})
    return parse_tool_result(result)


async def run_smoke_test(url: str, user: str, password: str, transport: str, workspace: str | None) -> None:
    print(f"Connecting to {url}")
    _, tools = await load_tools(url, user, password, transport)

    names = sorted(tool.name for tool in tools)
    print(f"Tools: {names}")
    assert "list_workspaces" in names, names
    assert "list_layers" in names, names

    workspaces = await invoke_named(tools, "list_workspaces")
    print("list_workspaces ->")
    print(json.dumps(workspaces, indent=2))
    assert isinstance(workspaces, list), f"expected a JSON array, got {type(workspaces)}: {workspaces!r}"

    layer_args: dict[str, str] = {}
    if workspace:
        layer_args["workspace"] = workspace
    elif workspaces and isinstance(workspaces[0], str):
        layer_args["workspace"] = workspaces[0]

    layers = await invoke_named(tools, "list_layers", layer_args)
    print(f"list_layers {layer_args} ->")
    print(json.dumps(layers, indent=2))
    assert isinstance(layers, list), f"expected a JSON array, got {type(layers)}: {layers!r}"
    for layer in layers:
        assert isinstance(layer, dict), layer
        for key in ("name", "workspace", "prefixedName"):
            assert key in layer, f"layer missing {key}: {layer}"

    print("LangChain smoke test passed.")


OPENROUTER_BASE_URL = "https://openrouter.ai/api/v1"
DEFAULT_OPENROUTER_MODEL = "openai/gpt-4o-mini"


def openrouter_api_key() -> str | None:
    return os.environ.get("OPENROUTER_API_KEY") or os.environ.get("OPENAI_API_KEY")


def openrouter_chat_model(model: str):
    from langchain_openai import ChatOpenAI

    api_key = openrouter_api_key()
    if not api_key:
        raise RuntimeError("Set OPENROUTER_API_KEY (or OPENAI_API_KEY with an OpenRouter key).")
    return ChatOpenAI(
        model=model,
        api_key=api_key,
        base_url=os.environ.get("OPENROUTER_BASE_URL", OPENROUTER_BASE_URL),
        default_headers={
            "HTTP-Referer": "https://github.com/mahdin75/geoserver-mcp",
            "X-Title": "GeoServer MCP Extension Test",
        },
    )


async def run_agent(url: str, user: str, password: str, transport: str, model: str) -> None:
    _, tools = await load_tools(url, user, password, transport)
    workspace = f"mcp_{uuid.uuid4().hex[:8]}"
    prompt = (
        f"List the GeoServer workspaces. "
        f"Then create a new workspace named {workspace}. "
        f"List the workspaces again to confirm {workspace} exists. "
        f"Then list layers in the first workspace that already existed before the create."
    )
    print(f"Agent prompt will create workspace {workspace!r}")
    llm = openrouter_chat_model(model)
    print(f"Running LangChain agent via OpenRouter model={model!r}")
    try:
        from langchain.agents import create_agent

        agent = create_agent(llm, tools)
        response = await agent.ainvoke({"messages": prompt})
    except ImportError:
        from langgraph.prebuilt import create_react_agent

        agent = create_react_agent(llm, tools)
        response = await agent.ainvoke({"messages": prompt})
    print(response)


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="LangChain smoke test for the GeoServer MCP extension")
    parser.add_argument(
        "--url",
        default=os.environ.get("GEOSERVER_MCP_URL", "http://localhost:8080/geoserver/mcp"),
        help="Streamable HTTP MCP endpoint",
    )
    parser.add_argument("--user", default=os.environ.get("GEOSERVER_USER", "admin"))
    parser.add_argument("--password", default=os.environ.get("GEOSERVER_PASSWORD", "geoserver"))
    parser.add_argument(
        "--transport",
        default=os.environ.get("GEOSERVER_MCP_TRANSPORT", "streamable_http"),
        help="langchain-mcp-adapters transport name (streamable_http or http)",
    )
    parser.add_argument("--workspace", default=os.environ.get("GEOSERVER_WORKSPACE"), help="Optional list_layers filter")
    parser.add_argument(
        "--agent",
        action="store_true",
        help="Also run a LangChain agent through OpenRouter (needs OPENROUTER_API_KEY)",
    )
    parser.add_argument(
        "--model",
        default=os.environ.get("OPENROUTER_MODEL", DEFAULT_OPENROUTER_MODEL),
        help="OpenRouter model id (default: openai/gpt-4o-mini)",
    )
    return parser.parse_args()


def format_exception(exc: BaseException) -> str:
    parts = [f"{type(exc).__name__}: {exc}"]
    sub = getattr(exc, "exceptions", None)
    if sub:
        for inner in sub:
            parts.append("  " + format_exception(inner).replace("\n", "\n  "))
    cause = exc.__cause__ or exc.__context__
    if cause is not None and cause is not exc:
        parts.append("caused by " + format_exception(cause))
    return "\n".join(parts)


def hint_for(exc: BaseException) -> str:
    name = type(exc).__name__
    text = str(exc).lower()
    if name in {"APIConnectionError", "ConnectError", "ConnectTimeout"} or "connection error" in text:
        return (
            "The GeoServer MCP smoke test already passed. --agent talks to OpenRouter, "
            "and this Python (3.10.0 / OpenSSL 1.1.1l) cannot complete HTTPS to Cloudflare. "
            "Use a newer Python 3.10.11+ or 3.11/3.12 venv, or skip --agent.\n"
        )
    if isinstance(exc, AssertionError):
        return ""
    if "503" in text or "404" in text or "connection" in text:
        return (
            "If GeoServer itself failed: use 2.28.x with gs-mcp, and prefer "
            "http://127.0.0.1:8080/geoserver/mcp (Python localhost can hit IPv6 and get 503).\n"
        )
    return ""


async def main() -> int:
    args = parse_args()
    try:
        await run_smoke_test(args.url, args.user, args.password, args.transport, args.workspace)
        if args.agent:
            if not openrouter_api_key():
                print("Skipping --agent: set OPENROUTER_API_KEY (OpenRouter key, not a raw OpenAI key).")
            else:
                await run_agent(args.url, args.user, args.password, args.transport, args.model)
    except Exception as exc:
        hint = hint_for(exc)
        print(f"LangChain smoke test failed.\n{hint}{format_exception(exc)}", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(asyncio.run(main()))
