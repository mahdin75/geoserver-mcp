"""
GeoServer MCP Client Example

This example demonstrates how to use the MCP client to connect to the GeoServer
MCP server and interact with GeoServer through the Model Context Protocol.
"""

import asyncio
import json
import os
import argparse
from typing import Dict, List, Any, Optional

# Import from the latest MCP SDK patterns
from mcp import ClientSession, StdioServerParameters
from mcp.client.stdio import stdio_client

# Parse command-line arguments
parser = argparse.ArgumentParser(description="GeoServer MCP Client Example")
parser.add_argument("--url", help="GeoServer URL (e.g., http://localhost:8080/geoserver)")
parser.add_argument("--user", help="GeoServer username")
parser.add_argument("--password", help="GeoServer password")
parser.add_argument("--server-url", help="Server URL argument to pass to the MCP server")
parser.add_argument("--server-user", help="Server username argument to pass to the MCP server")
parser.add_argument("--server-password", help="Server password argument to pass to the MCP server")
args = parser.parse_args()

# Configuration - adjust these values to match your environment
GEOSERVER_URL = args.url or os.environ.get("GEOSERVER_URL", "http://localhost:8080/geoserver")
GEOSERVER_USER = args.user or os.environ.get("GEOSERVER_USER", "admin")
GEOSERVER_PASSWORD = args.password or os.environ.get("GEOSERVER_PASSWORD", "geoserver")

# Create server parameters for stdio connection
server_args = []
if args.server_url:
    server_args.extend(["--url", args.server_url])
if args.server_user:
    server_args.extend(["--user", args.server_user])
if args.server_password:
    server_args.extend(["--password", args.server_password])

server_params = StdioServerParameters(
    command="geoserver-mcp-server",  # Command to start the MCP server
    args=server_args,                # Command line arguments for the server
    env={                            # Environment variables for the server
        "GEOSERVER_URL": GEOSERVER_URL,
        "GEOSERVER_USER": GEOSERVER_USER,
        "GEOSERVER_PASSWORD": GEOSERVER_PASSWORD,
    },
)

def print_json(obj: Any) -> None:
    """Pretty print JSON objects."""
    print(json.dumps(obj, indent=2))

async def run():
    """Run the GeoServer MCP client example."""
    print("\n🌍 Starting GeoServer MCP Client Example\n")
    
    async with stdio_client(server_params) as (read, write):
        async with ClientSession(read, write) as session:
            # Initialize the connection
            print("Initializing connection to GeoServer MCP server...")
            await session.initialize()
            print("✅ Connection initialized\n")

            # List available resources
            print("📚 Listing available resources...")
            resources = await session.list_resources()
            print(f"Found {len(resources.resources)} resources:")
            for resource in resources.resources:
                print(f"  - {resource.uri}")
            print()

            # List available tools
            print("🔧 Listing available tools...")
            tools = await session.list_tools()
            print(f"Found {len(tools.tools)} tools:")
            for tool in tools.tools:
                print(f"  - {tool.name}: {tool.description}")
            print()

            # Example 1: List workspaces
            print("🗂️  Example 1: Listing workspaces")
            print("--------------------------------")
            try:
                workspaces_result = await session.call_tool("list_workspaces", {})
                if workspaces_result.isError:
                    print(f"❌ Error: {workspaces_result.content[0].text}")
                else:
                    # The list_workspaces tool returns a list directly, not a JSON string
                    workspaces = workspaces_result.content[0].text
                    if isinstance(workspaces, str):
                        try:
                            workspaces = json.loads(workspaces)
                        except json.JSONDecodeError:
                            # If it's not JSON, it might be a comma-separated list
                            workspaces = [w.strip() for w in workspaces.strip('[]').split(',')]
                    
                    print(f"Found {len(workspaces)} workspaces:")
                    for workspace in workspaces:
                        print(f"  - {workspace}")
                print()
            except Exception as e:
                print(f"❌ Error listing workspaces: {e}")
                print()

            # Example 2: Get layer information
            print("🗃️  Example 2: Getting layer information")
            print("-------------------------------------")
            try:
                layer_info_result = await session.call_tool(
                    "get_layer_info", 
                    {"workspace": "topp", "layer": "states"}
                )
                if layer_info_result.isError:
                    print(f"❌ Error: {layer_info_result.content[0].text}")
                else:
                    print("Layer information:")
                    print_json(json.loads(layer_info_result.content[0].text))
                print()
            except Exception as e:
                print(f"❌ Error getting layer info: {e}")
                print()

            # Example 3: Query features
            print("🔍 Example 3: Querying features")
            print("-----------------------------")
            try:
                query_result = await session.call_tool(
                    "query_features",
                    {
                        "workspace": "topp",
                        "layer": "states",
                        "filter": "PERSONS > 10000000",
                        "properties": ["STATE_NAME", "PERSONS"],
                        "max_features": 3
                    }
                )
                if query_result.isError:
                    print(f"❌ Error: {query_result.content[0].text}")
                else:
                    features_data = json.loads(query_result.content[0].text)
                    features = features_data.get('features', [])
                    print(f"Found {len(features)} features:")
                    print_json(features_data)
                print()
            except Exception as e:
                print(f"❌ Error querying features: {e}")
                print()

            # Example 4: Generate a map
            print("🗺️  Example 4: Generating a map")
            print("-----------------------------")
            try:
                map_result = await session.call_tool(
                    "generate_map",
                    {
                        "layers": ["topp:states"],
                        "styles": ["population"],
                        "bbox": [-124.73, 24.96, -66.97, 49.37],
                        "width": 800,
                        "height": 600,
                        "format": "png"
                    }
                )
                if map_result.isError:
                    print(f"❌ Error: {map_result.content[0].text}")
                else:
                    map_data = json.loads(map_result.content[0].text)
                    print("Map generated successfully:")
                    print_json(map_data)
                    print(f"\nMap URL: {map_data.get('url')}")
                print()
            except Exception as e:
                print(f"❌ Error generating map: {e}")
                print()
                
            # Example 5: Create a workspace
            print("📁 Example 5: Creating a workspace")
            print("--------------------------------")
            try:
                create_result = await session.call_tool(
                    "create_workspace",
                    {"workspace": "demo-workspace"}
                )
                if create_result.isError:
                    print(f"❌ Error: {create_result.content[0].text}")
                else:
                    print("Workspace creation result:")
                    print_json(json.loads(create_result.content[0].text))
                print()
            except Exception as e:
                print(f"❌ Error creating workspace: {e}")
                print()
                
            # Example 6: Access a catalog resource
            print("📋 Example 6: Accessing a catalog resource")
            print("----------------------------------------")
            try:
                resource_content, mime_type = await session.read_resource(
                    "geoserver://catalog/workspaces"
                )
                print(f"Resource content (mime-type: {mime_type}):")
                if isinstance(resource_content, list):
                    for content in resource_content:
                        if hasattr(content, 'text'):
                            if mime_type == "application/json":
                                try:
                                    data = json.loads(content.text)
                                    print_json(data)
                                except json.JSONDecodeError:
                                    print(content.text)
                            else:
                                print(content.text)
                else:
                    print(resource_content)
                print()
            except Exception as e:
                print(f"❌ Error accessing resource: {e}")
                print()

            print("🏁 GeoServer MCP Client Example completed!")

if __name__ == "__main__":
    asyncio.run(run())
