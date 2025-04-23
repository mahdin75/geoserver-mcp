# GeoServer MCP Server

A Model Context Protocol (MCP) server implementation that connects LLMs to the GeoServer REST API, enabling AI assistants to interact with geospatial data and services.

## Overview

This MCP server provides a bridge between AI assistants and [GeoServer's REST API](https://docs.geoserver.org/latest/en/user/rest/index.html). It allows Large Language Models to:

- Query and manipulate GeoServer workspaces, layers, and styles
- Execute spatial queries on vector data
- Generate map visualizations
- Access OGC-compliant web services (WMS, WFS)

## Implementation Status

> **Note:** The current implementation includes the full MCP server architecture with all endpoints defined and fully functional using the `geo.Geoserver` library to interact with the GeoServer REST API.

## Getting Started

### Prerequisites

- Python 3.10+
- Running GeoServer instance with REST API enabled
- MCP-compatible client (like Claude Desktop)
- `geoserver-rest` package installed (`pip install geoserver-rest`)

### Installation

1. Install the package:

```bash
pip install -e .
```

2. Configure your connection to GeoServer using environment variables:

For Linux/Mac:

```bash
export GEOSERVER_URL="http://localhost:8080/geoserver"
export GEOSERVER_USER="admin"
export GEOSERVER_PASSWORD="geoserver"
```

For Windows PowerShell:

```powershell
$env:GEOSERVER_URL="http://localhost:8080/geoserver"
$env:GEOSERVER_USER="admin"
$env:GEOSERVER_PASSWORD="geoserver"
```

3. Start the server:

```bash
geoserver-mcp-server
```

You can also provide command-line arguments to configure the server:

```bash
geoserver-mcp-server --url http://localhost:8080/geoserver --user admin --password geoserver --debug
```

The server will start and listen for MCP requests on the standard input/output.

### Testing with Example Client

To test the server using the example client:

1. Ensure you have installed the package:

```bash
pip install -e .
```

2. Run the example client:

```bash
python examples/client.py
```

You can also provide command-line arguments to configure the client and server:

```bash
python examples/client.py --url http://localhost:8080/geoserver --user admin --password geoserver
```

Or pass arguments to the server:

```bash
python examples/client.py --server-url http://localhost:8080/geoserver --server-user admin --server-password geoserver
```

The client will connect to the GeoServer MCP server, list available resources and tools, and demonstrate key functionality like listing workspaces, querying features, and generating maps.

Make sure the GeoServer MCP server is installed and accessible, and that your environment variables are set correctly.

## MCP Integration

### Configuring Claude Desktop

To use this server with Claude Desktop:

1. Edit Claude Desktop's configuration file:

   - On MacOS: `~/Library/Application\ Support/Claude/claude_desktop_config.json`
   - On Windows: `%APPDATA%/Claude/claude_desktop_config.json`

2. Add the server configuration:

```json
"mcpServers": {
  "geoserver-mcp-server": {
    "command": "geoserver-mcp-server",
    "args": [
      "--url",
      "http://localhost:8080/geoserver",
      "--user",
      "admin",
      "--password",
      "geoserver"
    ]
  }
}
```

### Configuring Cursor

To use this server with Cursor:

1. Create or edit the `.cursor/mcp.json` file in your project root:

```json
{
  "mcpServers": {
    "geoserver-mcp-server": {
      "command": "geoserver-mcp-server",
      "args": [
        "--url",
        "http://localhost:8080/geoserver",
        "--user",
        "admin",
        "--password",
        "geoserver"
      ]
    }
  }
}
```

2. Make sure the `geoserver-mcp-server` command is available in your PATH by installing the package:

```bash
pip install -e .
```

3. Restart Cursor to apply the configuration.

### Troubleshooting MCP Integration

If you encounter issues with the GeoServer MCP server:

1. **Check if the server is installed**: Make sure the `geoserver-mcp-server` command is available in your PATH.

2. **Verify GeoServer is running**: Ensure your GeoServer instance is running and accessible at the URL you've specified.

3. **Check logs**: Look for any error messages in the application logs.

4. **Test the server directly**: Try running the server directly from the command line:

   ```bash
   geoserver-mcp-server --url http://localhost:8080/geoserver --user admin --password geoserver
   ```

5. **Restart the application**: After making changes to the configuration file, restart the application to ensure it picks up the new configuration.

## Implemented Features

### MCP Resources

The server exposes GeoServer data through these MCP resources:

| Resource URI                                     | Description               | Status         |
| ------------------------------------------------ | ------------------------- | -------------- |
| `geoserver://catalog/workspaces`                 | List available workspaces | ✅ Implemented |
| `geoserver://catalog/layers/{workspace}/{layer}` | Access layer information  | ✅ Implemented |
| `geoserver://services/wms/{request}`             | Access WMS services       | ✅ Implemented |
| `geoserver://services/wfs/{request}`             | Access WFS services       | ✅ Implemented |

### MCP Tools

The server provides these tools for LLMs to interact with GeoServer:

#### Catalog Management Tools

| Tool Name          | Description                               | Status         |
| ------------------ | ----------------------------------------- | -------------- |
| `list_workspaces`  | Get available workspaces                  | ✅ Implemented |
| `create_workspace` | Create a new workspace                    | ✅ Implemented |
| `get_layer_info`   | Get detailed layer metadata               | ✅ Implemented |
| `list_layers`      | List layers in a workspace                | ✅ Implemented |
| `create_layer`     | Create a new layer                        | ✅ Implemented |
| `delete_resource`  | Remove resources (workspace, layer, etc.) | ✅ Implemented |

#### Data Operation Tools

| Tool Name        | Description                        | Status         |
| ---------------- | ---------------------------------- | -------------- |
| `query_features` | Execute CQL queries on vector data | ✅ Implemented |

#### Visualization Tools

| Tool Name      | Description              | Status         |
| -------------- | ------------------------ | -------------- |
| `generate_map` | Create styled map images | ✅ Implemented |
| `create_style` | Define new SLD styles    | ✅ Implemented |

## Example Usage

Here's how an LLM can interact with GeoServer through this MCP server:

### Listing Available Workspaces

```
Tool: list_workspaces
Parameters: {}
```

Example response:

```json
["default", "demo", "topp", "tiger", "sf"]
```

Here's how the workspaces appear in the MCP client:

![List of GeoServer Workspaces](demo/list_workspaces.png)

The screenshot shows the actual workspaces available in the GeoServer instance, including: mahdi, demo-workspace, cite, tiger, nurc, sde, it.geosolutions, topp, and sf. These workspaces serve as containers for organizing your GeoServer resources.

### Getting Layer Information

```
Tool: get_layer_info
Parameters: {
  "workspace": "topp",
  "layer": "states"
}
```

Example response:

```json
{
  "name": "states",
  "workspace": "topp",
  "type": "FeatureType",
  "enabled": true,
  "metadata": {
    "title": "states Layer",
    "abstract": "This is the states layer in the topp workspace",
    "keywords": ["sample", "geoserver", "layer"]
  },
  "bbox": {
    "minx": -180.0,
    "miny": -90.0,
    "maxx": 180.0,
    "maxy": 90.0,
    "crs": "EPSG:4326"
  }
}
```

### Querying Features

```
Tool: query_features
Parameters: {
  "workspace": "topp",
  "layer": "states",
  "filter": "PERSONS > 10000000",
  "properties": ["STATE_NAME", "PERSONS"]
}
```

Example response:

```json
{
  "type": "FeatureCollection",
  "features": [
    {
      "type": "Feature",
      "id": "states.1",
      "properties": {
        "STATE_NAME": "California",
        "PERSONS": 29760021
      }
    },
    {
      "type": "Feature",
      "id": "states.2",
      "properties": {
        "STATE_NAME": "Texas",
        "PERSONS": 16986510
      }
    }
  ]
}
```

### Generating a Map

```
Tool: generate_map
Parameters: {
  "layers": ["topp:states"],
  "styles": ["population"],
  "bbox": [-124.73, 24.96, -66.97, 49.37],
  "width": 800,
  "height": 600,
  "format": "png"
}
```

Example response:

```json
{
  "url": "http://localhost:8080/geoserver/wms?SERVICE=WMS&VERSION=1.3.0&REQUEST=GetMap&FORMAT=image/png&LAYERS=topp:states&STYLES=population&CRS=EPSG:4326&BBOX=-124.73,24.96,-66.97,49.37&WIDTH=800&HEIGHT=600",
  "width": 800,
  "height": 600,
  "format": "png",
  "layers": ["topp:states"],
  "styles": ["population"],
  "bbox": [-124.73, 24.96, -66.97, 49.37]
}
```

## Planned Features

Future development will focus on expanding the available tools and resources to cover more GeoServer REST API functionality:

- Coverage and raster data management
- Security and access control
- Advanced styling capabilities
- WPS processing operations
- GeoWebCache integration

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Related Projects

- [Model Context Protocol](https://github.com/modelcontextprotocol/python-sdk)
- [GeoServer REST API](https://docs.geoserver.org/latest/en/user/rest/index.html)
- [GeoServer REST Python Client](https://github.com/gicait/geoserver-rest)

```

```
