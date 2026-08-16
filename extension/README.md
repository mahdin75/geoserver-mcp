# GeoServer MCP Extension

A native GeoServer extension that exposes GeoServer catalog operations through the [Model Context Protocol](https://modelcontextprotocol.io/) (MCP).

This is a **second deployment option** for GeoServer MCP. It does not replace the Python MCP server in this repository.

```text
                    GeoServer MCP
                         │
             ┌───────────┴───────────┐
             │                       │
      Python MCP Server       GeoServer Extension
             │                       │
             ▼                       ▼
        GeoServer                GeoServer
             │                       │
             └───────────┬───────────┘
                         │
                    MCP Interface
                         │
                         ▼
                     AI Agents
```

## What it is

After you copy the extension JAR into GeoServer and restart, the GeoServer process itself serves a remote MCP endpoint. Clients connect with Streamable HTTP. You do not install Python, run a sidecar MCP process, or use Docker to expose MCP.

```text
AI Agent
   ↓
Remote MCP (Streamable HTTP)
   ↓
GeoServer + MCP Extension
```

## Architecture

```text
GeoServer
    +
MCP Extension  (GeoServerFilter + Catalog API)
    ↓
https://example.com/geoserver/mcp
    ↓
AI Agent
```

The extension is a standard GeoServer plugin:

1. Maven builds a JAR that contains `applicationContext.xml`.
2. GeoServer loads every `applicationContext.xml` on the classpath at startup.
3. The extension registers a `GeoServerFilter` bean. GeoServer's existing `SpringDelegatingFilter` (already mapped to `/*` in `web.xml`) picks that bean up. No `web.xml` edit is required.
4. Requests to `/mcp` are handled as MCP Streamable HTTP. Other GeoServer URLs are unchanged.
5. Tools call the injected `catalog` bean (the secured catalog), not the GeoServer REST API over HTTP.

The official MCP Java SDK is not used. That SDK targets Jakarta Servlet 6, while GeoServer 2.28.x still uses `javax.servlet`. The extension implements the Streamable HTTP JSON subset against the current MCP specification instead.

## Compatibility

| Item | Support in 0.1.0 |
| --- | --- |
| GeoServer | **2.28.x**, compiled against **2.28.5** |
| Java | 17 or 21 (same baseline as GeoServer 2.28) |
| Servlet API | Java EE `javax.servlet` (GeoServer 2) |
| MCP transport | Streamable HTTP, JSON request/response |
| MCP protocol versions | `2025-03-26`, `2025-06-18`, `2025-11-25`, `2026-07-28` |
| GeoServer 2.26 / 2.27 | Not claimed |
| GeoServer 3.0 | Not claimed (Jakarta EE; needs a separate build) |

Binary compatibility is expected across GeoServer 2.28.x patch releases because the extension only uses stable Catalog and security APIs. That has been unit-tested, not installation-tested on every 2.28 patch.

Do not install this JAR into GeoServer 3.x. GeoServer 3 uses Jakarta Servlet 6.1; this build will not load.

## Installation

### Build

Requires JDK 17+ and Maven 3.8+.

```bash
cd extension
mvn -DskipTests package
```

Artifacts:

| File | Location |
| --- | --- |
| Extension JAR | `extension/target/gs-mcp-0.1.0.jar` |
| Plugin zip | `extension/target/geoserver-2.28-mcp-plugin.zip` |

The zip contains the JAR plus license/example files. Jackson is not bundled; GeoServer 2.28 already ships Jackson 2 on the webapp classpath.

### Install into GeoServer 2.28.x

1. Confirm the GeoServer version: **About & Status → About GeoServer**. You need 2.28.x.
2. Stop GeoServer.
3. Copy `gs-mcp-0.1.0.jar` into:
   - Binary/Jetty: `<GEOSERVER_ROOT>/webapps/geoserver/WEB-INF/lib`
   - Tomcat: `<CATALINA_BASE>/webapps/geoserver/WEB-INF/lib`
4. Start GeoServer.
5. Check **About & Status → Modules** for **GeoServer MCP Extension** (`gs-mcp`).
6. Call the endpoint (see below).

This is the same install method GeoServer documents for other extensions: matching series, JARs in `WEB-INF/lib`, restart.

> **Note:** Do not copy this JAR into an older GeoServer such as **2.20.x**. Those installs typically run **Java 11**. This plugin is compiled for **Java 17** (class file version 61) and GeoServer **2.28.x**. Putting it in `WEB-INF/lib` on 2.20.4 makes Spring fail to start the webapp: `/geoserver/web/` returns HTTP **503 Service Unavailable**, and the log shows `UnsupportedClassVersionError` (`class file version 61.0` vs runtime `55.0`). Remove the JAR from `WEB-INF/lib` and restart to recover. Test the extension on a separate GeoServer 2.28.x instance; leave an existing 2.20.x install untouched.

## MCP endpoint

| | |
| --- | --- |
| URL | `https://<host>/<webapp>/mcp` |
| Typical local URL | `http://localhost:8080/geoserver/mcp` |
| HTTP methods | `POST` (required), `OPTIONS` (CORS preflight) |
| Transport | [Streamable HTTP](https://modelcontextprotocol.io/specification/2025-03-26/basic/transports) |
| Body | JSON-RPC 2.0 |
| Success response | `Content-Type: application/json` |
| Notifications | HTTP `202` with an empty body |
| `GET` / `DELETE` | HTTP `405` (no standalone SSE stream, no protocol sessions) |

The path is `/mcp` on the GeoServer web application. If GeoServer is mounted at `/geoserver`, the public URL is `/geoserver/mcp`. That matches the usual `https://example.com/geoserver/mcp` layout.

Avoid naming a workspace `mcp`. GeoServer's advanced dispatch can treat the first path segment as a workspace name.

## Configuration

The GeoServer base URL is not a setting. The extension is already inside GeoServer.

Optional file in the GeoServer data directory:

```text
$GEOSERVER_DATA_DIR/mcp.properties
```

```properties
mcp.enabled=true
mcp.path=/mcp
mcp.requireAuthentication=true
mcp.cors.enabled=false
mcp.cors.allowOrigin=*
mcp.maxListResults=1000
```

Environment variables and Java system properties override the file:

| Setting | File key | Environment / system property | Default |
| --- | --- | --- | --- |
| Enable MCP | `mcp.enabled` | `GEOSERVER_MCP_ENABLED` | `true` |
| Endpoint path | `mcp.path` | `GEOSERVER_MCP_PATH` | `/mcp` |
| Require a non-anonymous login | `mcp.requireAuthentication` | `GEOSERVER_MCP_REQUIRE_AUTHENTICATION` | `true` |
| CORS | `mcp.cors.enabled` | `GEOSERVER_MCP_CORS_ENABLED` | `false` |
| CORS origin | `mcp.cors.allowOrigin` | `GEOSERVER_MCP_CORS_ALLOW_ORIGIN` | `*` |
| Max workspaces/layers per list | `mcp.maxListResults` | `GEOSERVER_MCP_MAX_LIST_RESULTS` | `1000` |

Restart GeoServer after changing `mcp.properties`. Logging uses the GeoServer / `java.util.logging` category `org.geoservermcp`.

## Security

This milestone is **read-only**. The extension does not expose:

- GeoServer administration
- arbitrary REST passthrough
- filesystem access
- catalog writes or configuration changes
- datastore credentials
- user management
- code execution

### Authentication

By default `mcp.requireAuthentication=true`. Unauthenticated calls receive HTTP `401` and `WWW-Authenticate: Basic realm="GeoServer MCP"`. Send the same credentials you would use for GeoServer services (HTTP Basic). GeoServer's default filter chain for `/**` already runs Basic auth before this filter.

To allow anonymous MCP access to **public** catalog objects:

```properties
mcp.requireAuthentication=false
```

Anonymous users still only see layers and workspaces that GeoServer data security allows for the anonymous role.

### Authorization

Tools use the secured `catalog` bean (`SecureCatalogImpl`). Layer and workspace lists are filtered with the current Spring Security `Authentication`. A user who cannot see a layer in WMS/WFS will not see it through MCP.

Required permissions for the proof-of-concept tools:

| Tool | GeoServer permission |
| --- | --- |
| `list_workspaces` | Read access to the workspaces that should appear |
| `list_layers` | Read access to the layers that should appear |

Administrators can optionally add a dedicated authentication filter chain for `/mcp/**` in **Security → Authentication → Filter Chains**. That is not required for the default Basic-auth behavior.

## Connecting an agent

Use any MCP client that supports **Streamable HTTP** (POST JSON to a single endpoint). The extension does not embed LangChain, LangGraph, or any other agent framework.

Replace host, credentials, and protocol version as needed.

### Generic MCP client (Python SDK)

```python
import asyncio
from mcp.client.streamable_http import streamablehttp_client
from mcp import ClientSession

async def main():
    url = "http://localhost:8080/geoserver/mcp"
    headers = {"Authorization": "Basic YWRtaW46Z2Vvc2VydmVy"}  # admin:geoserver
    async with streamablehttp_client(url, headers=headers) as (read, write, _):
        async with ClientSession(read, write) as session:
            await session.initialize()
            tools = await session.list_tools()
            print([tool.name for tool in tools.tools])
            result = await session.call_tool("list_layers", {"workspace": "topp"})
            print(result)

asyncio.run(main())
```

### curl

```bash
# initialize
curl -s -u admin:geoserver \
  -H "Content-Type: application/json" \
  -H "Accept: application/json, text/event-stream" \
  -H "MCP-Protocol-Version: 2025-03-26" \
  -d '{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2025-03-26","capabilities":{},"clientInfo":{"name":"curl","version":"1.0"}}}' \
  http://localhost:8080/geoserver/mcp

# tools/list
curl -s -u admin:geoserver \
  -H "Content-Type: application/json" \
  -H "Accept: application/json, text/event-stream" \
  -H "MCP-Protocol-Version: 2025-03-26" \
  -d '{"jsonrpc":"2.0","id":2,"method":"tools/list"}' \
  http://localhost:8080/geoserver/mcp

# list_layers
curl -s -u admin:geoserver \
  -H "Content-Type: application/json" \
  -H "Accept: application/json, text/event-stream" \
  -H "MCP-Protocol-Version: 2025-03-26" \
  -d '{"jsonrpc":"2.0","id":3,"method":"tools/call","params":{"name":"list_layers","arguments":{}}}' \
  http://localhost:8080/geoserver/mcp
```

### LangChain

A runnable smoke test lives at [`examples/langchain_mcp_test.py`](examples/langchain_mcp_test.py). It loads the MCP tools through `langchain-mcp-adapters` and calls `list_workspaces` / `list_layers` (no LLM required).

```bash
pip install -r extension/examples/requirements-langchain.txt
python extension/examples/langchain_mcp_test.py
```

Optional flags: `--url`, `--user`, `--password`, `--workspace`. Pass `--agent` (and set `OPENROUTER_API_KEY`) to also run a LangChain agent through OpenRouter. `--model` defaults to `openai/gpt-4o-mini`.

```python
from langchain_mcp_adapters.client import MultiServerMCPClient

client = MultiServerMCPClient(
    {
        "geoserver": {
            "transport": "streamable_http",
            "url": "http://localhost:8080/geoserver/mcp",
            "headers": {"Authorization": "Basic YWRtaW46Z2Vvc2VydmVy"},
        }
    }
)
tools = await client.get_tools()
```

Adapter package names and connection dict keys change across `langchain-mcp-adapters` releases. If `transport` is rejected, try `http` instead of `streamable_http`, or pass `--transport http` to the smoke test. The GeoServer URL and HTTP Basic header stay the same.

### LangGraph

```python
from langchain_mcp_adapters.client import MultiServerMCPClient
from langgraph.prebuilt import create_react_agent

client = MultiServerMCPClient(
    {
        "geoserver": {
            "transport": "streamable_http",
            "url": "http://localhost:8080/geoserver/mcp",
            "headers": {"Authorization": "Basic YWRtaW46Z2Vvc2VydmVy"},
        }
    }
)
tools = await client.get_tools()
agent = create_react_agent(model, tools)
```

`model` is your existing chat model. LangGraph only consumes the MCP tool list; it has no GeoServer-specific configuration.

## Implemented tools (0.1.0)

| Tool | Python MCP | Java extension | Priority |
| --- | --- | --- | --- |
| `list_workspaces` | existing | implemented | P0 |
| `list_layers` | existing | implemented | P0 |

Tool names and descriptions match the Python server. `list_workspaces` returns a JSON array of workspace name strings. `list_layers` accepts optional `workspace` and returns a JSON array of objects:

```json
[
  {
    "name": "states",
    "workspace": "topp",
    "prefixedName": "topp:states",
    "enabled": true,
    "type": "VECTOR"
  }
]
```

The Python server currently forwards GeoServer REST payloads from `geoserver-rest`. The Java tool uses the Catalog API and returns this stable shape instead of replaying REST XML/JSON. Agents that only use tool names and the `workspace` argument can switch endpoints without changes. Agents that parse the exact REST document from Python `list_layers` will need to read `name` / `workspace` / `prefixedName`.

## Future tool mapping

Do not treat this table as implemented. It is the planned compatibility map with the Python server.

| Tool | Python MCP | Java extension | Priority | Notes |
| --- | --- | --- | --- | --- |
| `list_workspaces` | existing | implemented | P0 | Catalog `getWorkspaces()` |
| `list_layers` | existing | implemented | P0 | Catalog `getLayers()` |
| `get_layer_info` | existing | planned | P0 | Catalog `LayerInfo` / `ResourceInfo` |
| `query_features` | existing | planned | P1 | GeoTools `FeatureSource`, not HTTP WFS |
| `generate_map` | existing | planned | P1 | WMS internals or a GetMap URL |
| `get_datastores` / `get_datastore` | existing | planned | P1 | Catalog stores |
| `get_coveragestores` / `get_coveragestore` | existing | planned | P1 | Catalog coverage stores |
| `get_layergroups` / `get_layergroup` | existing | planned | P1 | Catalog layer groups |
| `get_featuretypes` / `get_feature_attribute` | existing | planned | P1 | FeatureTypeInfo |
| `get_version` / `get_status` | existing | planned | P1 | Read-only About/status |
| `create_*` / `delete_*` / `publish_*` | existing | not planned for default | — | Writes stay out of the default extension |
| `create_user` / `delete_user` / `reload_geoserver` | existing | will not port | — | Administration; too broad |

Write tools, if ever added, must be opt-in and still go through GeoServer security.

## Testing

Unit tests mock the Catalog and HTTP layer. They do not start GeoServer.

```bash
cd extension
mvn test
```

Covered:

- Spring `applicationContext.xml` is present and declares the filter
- MCP `initialize`, `tools/list`, `tools/call`
- `list_layers` / `list_workspaces`
- invalid JSON, unknown methods, unknown tools
- authentication gate (anonymous vs authenticated)
- catalog workspace filter and unknown workspace errors

### Against a real GeoServer 2.28

1. Install the JAR as above.
2. Run the `curl` initialize / `tools/list` / `list_layers` sequence.
3. Or run the LangChain smoke test:

```bash
pip install -r extension/examples/requirements-langchain.txt
python extension/examples/langchain_mcp_test.py
```

4. Confirm a non-admin user only sees permitted layers.
5. Confirm `GET /geoserver/mcp` returns 405.
6. Confirm unauthenticated POST returns 401 while `mcp.requireAuthentication=true`.

A full Testcontainers GeoServer run is not part of the default `mvn test` lifecycle. Use a 2.28 install or Docker image, copy the JAR into `WEB-INF/lib`, and restart. This script will fail against GeoServer 2.20.x.

## Versioning

| Artifact | Coordinates | Current version |
| --- | --- | --- |
| Python package | PyPI `geoserver-mcp` | independent (`0.4.0` today) |
| GeoServer extension | Maven `org.geoservermcp:gs-mcp` | `0.1.0` |

Adding the extension does **not** bump the Python package. Release the JAR from GitHub Releases. Do not publish the extension to PyPI. Maven Central is not required for this distribution model.

## GitHub Release

1. `cd extension && mvn -DskipTests package` (run tests first on a release build: `mvn verify`).
2. Create a GitHub Release such as `extension-0.1.0`.
3. Attach `target/geoserver-2.28-mcp-plugin.zip` and `target/gs-mcp-0.1.0.jar`.
4. State the supported GeoServer series in the release notes (`2.28.x`).

Maven is used only to **build** the extension and to **download compile-time dependencies** from OSGeo (`repo.osgeo.org`). Administrators install the GitHub artifact, not a Maven Central coordinate.

## License

GNU GPL v2.0 or later. GeoServer is GPL, and this module links GeoServer APIs. The Python MCP server in the parent repository remains MIT-licensed.

## Limitations and future work

- Proof of concept: two read-only catalog tools.
- No MCP resources, prompts, or SSE GET stream.
- No GeoServer 3.0 / Jakarta build.
- No web admin page; configuration is properties / env.
- `list_layers` JSON is Catalog-structured, not a copy of the Python REST payload.
- Not published to Maven Central.
- Workspace named `mcp` can clash with the endpoint path.
