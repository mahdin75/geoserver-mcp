<img src="https://badge.mcpx.dev?type=server" title="MCP Server"/>
[![PyPI](https://img.shields.io/pypi/v/geoserver-mcp)](https://pypi.org/project/geoserver-mcp/) [![Downloads](https://static.pepy.tech/personalized-badge/geoserver-mcp?period=total&units=international_system&left_color=grey&right_color=blue&left_text=PyPI%20downloads)](https://pepy.tech/project/geoserver-mcp)

# GeoServer MCP Server

<p align="center">
A Model Context Protocol (MCP) server implementation that connects Large Language Models (LLMs) to the GeoServer REST API, enabling AI assistants to interact with geospatial data and services.

</p>

<div align="center">
  <img src="docs/geoserver-mcp.png" alt="GeoServer MCP Server Logo" width="400"/>
</div>

> Version 0.5.0 (Beta) is under active development and will be released shortly. We are open to contributions and welcome developers to join us in building this project.

## 🎥 Demo

<div align="center">
  <img src="docs/demo/list_workspaces.png" alt="GeoServer MCP Server Demo" width="400"/>
</div>

## 📋 Table of Contents

- [Features](#-features)
- [Deployment Options](#-deployment-options)
- [Prerequisites](#-prerequisites)
- [Installation](#️-installation)
  - [Docker Installation](#️-installation-docker)
  - [pip Installation](#️-installation-pip)
  - [Development Installation](#️-development-installation)
- [File Storage and `--storage` Usage](#file-storage-and---storage-usage)
- [Available Tools](#️-available-tools)
  - [Resource Endpoints](#️-resource-endpoints)
  - [Workspace Management](#️-workspace-management)
  - [Datastore & Coveragestore Management](#️-datastore--coveragestore-management)
  - [Layer Management](#️-layer-management)
  - [Layer Group Management](#️-layer-group-management)
  - [User & User Group Management](#️-user--user-group-management)
  - [Feature Type & Attribute Management](#️-feature-type--attribute-management)
  - [Style Management](#️-style-management)
  - [System & Service Operations](#️-system--service-operations)
  - [Style XML Utilities](#️-style-xml-utilities)
- [Client Development](#️-client-development)
  - [List Workspaces](#list-workspaces)
  - [Get Layer Information](#get-layer-information)
  - [Query Features](#query-features)
  - [Generate Map](#generate-map)
- [Planned Features](#-planned-features)
- [Contributing](#-contributing)
- [License](#-license)
- [Related Projects](#-related-projects)
- [Support](#-support)
- [Badges](#-badges)

## 🚀 Features

- 🔍 Query and manipulate GeoServer workspaces, layers, and styles
- 🗺️ Execute spatial queries on vector data
- 🎨 Generate map visualizations
- 🌐 Access OGC-compliant web services (WMS, WFS)
- 🛠️ Easy integration with MCP-compatible clients

## 🚀 Deployment Options

GeoServer MCP can run in two ways. They share the same product idea (MCP tools over GeoServer) but are **separate artifacts**. The Python package is unchanged.

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

### Python MCP Server

Run GeoServer MCP separately (pip, Docker, or Smithery). The process speaks MCP to the agent and calls the GeoServer REST API. This is the original, currently published deployment.

See [Installation](#️-installation) below.

### GeoServer Extension

Install the GeoServer MCP Extension directly into GeoServer and expose a remote MCP endpoint at `/geoserver/mcp`. No Python sidecar is required. Targets GeoServer **2.28.x**.

See [`extension/README.md`](extension/README.md) for architecture, installation, configuration, security, and client examples.

## 📋 Prerequisites

- Python 3.10 or higher
- Running GeoServer instance with REST API enabled
- MCP-compatible client (like Claude Desktop or Cursor)
- Internet connection for package installation

## 🛠️ Installation

Choose the installation method that best suits your needs:

### Installing via Smithery

To install GeoServer MCP Server for Claude Desktop automatically via [Smithery](https://smithery.ai/server/@mahdin75/geoserver-mcp):

```bash
npx -y @smithery/cli install @mahdin75/geoserver-mcp --client claude
```

### 🛠️ Installation (Docker)

The Docker installation is the quickest and most isolated way to run the GeoServer MCP server. It's ideal for:

- Quick testing and evaluation
- Production deployments
- Environments where you want to avoid Python dependencies
- Consistent deployment across different systems

1. Run geoserver-mcp:

```bash
docker pull mahdin75/geoserver-mcp
docker run -d mahdin75/geoserver-mcp
```

2. Configure the clients:

If you are using Claude Desktop, edit `claude_desktop_config.json`
If you are using Cursor, Create `.cursor/mcp.json`

```json
{
  "mcpServers": {
    "geoserver-mcp": {
      "command": "docker",
      "args": [
        "run",
        "-i",
        "--rm",
        "-e",
        "GEOSERVER_URL=http://localhost:8080/geoserver",
        "-e",
        "GEOSERVER_USER=admin",
        "-e",
        "GEOSERVER_PASSWORD=geoserver",
        "-p",
        "8080:8080",
        "mahdin75/geoserver-mcp"
      ]
    }
  }
}
```

### 🛠️ Installation (pip)

The pip installation is recommended for most users who want to run the server directly on their system. This method is best for:

- Regular users who want to run the server locally
- Systems where you have Python 3.10+ installed
- Users who want to customize the server configuration
- Development and testing purposes

1. Install uv package manager.

```bash
pip install uv
```

2. Create the Virtual Environment (Python 3.10+):

**Linux/Mac:**

```bash
uv venv --python=3.10
```

**Windows PowerShell:**

```bash
uv venv --python=3.10
```

3. Install the package using pip:

```bash
uv pip install geoserver-mcp
```

4. Configure GeoServer connection:

**Linux/Mac:**

```bash
export GEOSERVER_URL="http://localhost:8080/geoserver"
export GEOSERVER_USER="admin"
export GEOSERVER_PASSWORD="geoserver"
```

**Windows PowerShell:**

```powershell
$env:GEOSERVER_URL="http://localhost:8080/geoserver"
$env:GEOSERVER_USER="admin"
$env:GEOSERVER_PASSWORD="geoserver"
```

5. Start the server:

If you are going to use Claude desktop you don't need this step. For cursor or your own custom client you should run the following code.

**Linux:**

```bash
source .venv/bin/activate

geoserver-mcp
```

or

```bash
source .venv/bin/activate

geoserver-mcp --url http://localhost:8080/geoserver --user admin --password geoserver --debug
```

**Windows PowerShell:**

```bash
.\.venv\Scripts\activate
geoserver-mcp
```

or

```bash
.\.venv\Scripts\activate
geoserver-mcp --url http://localhost:8080/geoserver --user admin --password geoserver --debug
```

6. Configure Clients:

If you are using Claude Desktop, edit `claude_desktop_config.json`
If you are using Cursor, Create `.cursor/mcp.json`

**Windows:**

```json
{
  "mcpServers": {
    "geoserver-mcp": {
      "command": "C:\\path\\to\\geoserver-mcp\\.venv\\Scripts\\geoserver-mcp",
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

**Linux:**

```json
{
  "mcpServers": {
    "geoserver-mcp": {
      "command": "/path/to/geoserver-mcp/.venv/bin/geoserver-mcp",
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

### 🛠️ Development installation

The development installation is designed for contributors and developers who want to modify the codebase. This method is suitable for:

- Developers contributing to the project
- Users who need to modify the source code
- Testing new features
- Debugging and development purposes

1. Install uv package manager.

```bash
pip install uv
```

2. Create the Virtual Environment (Python 3.10+):

```bash
uv venv --python=3.10
```

3. Install the package using pip:

```bash
uv pip install -e .
```

4. Configure GeoServer connection:

**Linux/Mac:**

```bash
export GEOSERVER_URL="http://localhost:8080/geoserver"
export GEOSERVER_USER="admin"
export GEOSERVER_PASSWORD="geoserver"
```

**Windows PowerShell:**

```powershell
$env:GEOSERVER_URL="http://localhost:8080/geoserver"
$env:GEOSERVER_USER="admin"
$env:GEOSERVER_PASSWORD="geoserver"
```

5. Start the server:

If you are going to use Claude desktop you don't need this step. For cursor or your own custom client you should run the following code.

**Linux:**

```bash
source .venv/bin/activate

geoserver-mcp
```

or

```bash
source .venv/bin/activate

geoserver-mcp --url http://localhost:8080/geoserver --user admin --password geoserver --debug
```

**Windows PowerShell:**

```bash
.\.venv\Scripts\activate
geoserver-mcp
```

or

```bash
.\.venv\Scripts\activate
geoserver-mcp --url http://localhost:8080/geoserver --user admin --password geoserver --debug
```

6. Configure Clients:

If you are using Claude Desktop, edit `claude_desktop_config.json`
If you are using Cursor, Create `.cursor/mcp.json`

**Windows:**

```json
{
  "mcpServers": {
    "geoserver-mcp": {
      "command": "C:\\path\\to\\geoserver-mcp\\.venv\\Scripts\\geoserver-mcp",
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

**Linux:**

```json
{
  "mcpServers": {
    "geoserver-mcp": {
      "command": "/path/to/geoserver-mcp/.venv/bin/geoserver-mcp",
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

## File Storage and --storage Usage

GeoServer MCP server supports an optional `--storage` flag to specify a base directory for all file read/write operations, such as uploading shapefiles, GeoTIFFs, or exporting results.

### Overview

- The `--storage` flag sets the root folder for file operations from all data-related tools.
- You may supply **relative paths** (relative to storage root) or **absolute paths** (bypassing the storage root) as arguments to relevant tools.
- If `--storage` is not set, paths are resolved as provided by the user (relative to working directory or absolute).

### CLI Example

```sh
python -m geoserver_mcp.main --storage D:/my/data/dir
```

This sets `D:/my/data/dir` as the base path for all files.

**Example tool call in Python:**

```python
# Will read from D:/my/data/dir/roads.zip if --storage is set to D:/my/data/dir
create_shp_datastore('workspace', 'datastore_name', 'roads.zip')
```

Absolute paths (e.g. 'C:/input/other.shp') are always used as-is.

### When Running in Docker

If using Docker, ensure the storage directory is mounted as a volume, e.g.:

```sh
docker run -v D:/my/data:/opt/data ...
```

Then launch the server with:

```sh
python -m geoserver_mcp.main --storage /opt/data
```

### Best Practices

- Use relative paths when interacting with the API/tools as it keeps your setup portable.
- For remote or container deployment, always ensure your file data is accessible within the container (use Docker volumes if needed).
- Check tool docstrings for which arguments use the storage system.

The `--storage` system streamlines file management for all users and makes deployment much more flexible!

## 🛠️ Available Tools

This section details all the available tools and resources exposed by the GeoServer MCP server. These tools allow LLMs to interact with GeoServer's REST API for comprehensive geospatial data management.

### 🌍 Resource Endpoints

Resource endpoints provide direct access to GeoServer resources via a URI pattern.

| Resource URI                                     | Description                            |
| :----------------------------------------------- | :------------------------------------- |
| `geoserver://catalog/workspaces`                 | List available workspaces              |
| `geoserver://catalog/layers/{workspace}/{layer}` | Get information about a specific layer |
| `geoserver://services/wms/{request}`             | Handle WMS resource requests           |
| `geoserver://services/wfs/{request}`             | Handle WFS resource requests           |

### 📦 Workspace Management

| Tool               | Description                            |
| :----------------- | :------------------------------------- |
| `list_workspaces`  | List available workspaces in GeoServer |
| `create_workspace` | Create a new workspace in GeoServer    |

### 📁 Datastore & Coveragestore Management

| Tool                    | Description                                      |
| :---------------------- | :----------------------------------------------- |
| `create_datastore`      | Create a new datastore in the given workspace    |
| `create_featurestore`   | Create a new featurestore in the given workspace |
| `create_gpkg_datastore` | Create a GeoPackage (GPKG) datastore             |
| `create_shp_datastore`  | Create an ESRI Shapefile datastore               |
| `create_coveragestore`  | Create a new coveragestore in a workspace        |
| `delete_coveragestore`  | Delete a coveragestore from a workspace          |
| `get_coveragestore`     | Get details about a single coveragestore         |
| `get_coveragestores`    | Get all coveragestores for a workspace           |
| `get_datastore`         | Get a specific datastore by name                 |
| `get_datastores`        | List all datastores in the given workspace       |

### 🗺️ Layer Management

| Tool              | Description                                                |
| :---------------- | :--------------------------------------------------------- |
| `get_layer_info`  | Get detailed information about a layer                     |
| `list_layers`     | List layers in GeoServer, optionally filtered by workspace |
| `create_layer`    | Create a new layer in GeoServer                            |
| `delete_resource` | Delete a resource from GeoServer (generic)                 |

### 🧩 Layer Group Management

| Tool                           | Description                                                           |
| :----------------------------- | :-------------------------------------------------------------------- |
| `create_layergroup`            | Create a new layer group with specific layers and (optionally) styles |
| `get_layergroup`               | Get a layer group from a workspace                                    |
| `get_layergroups`              | List all layer groups in a workspace                                  |
| `add_layer_to_layergroup`      | Add a specific layer to a layer group                                 |
| `remove_layer_from_layergroup` | Remove a layer from a group                                           |
| `delete_layergroup`            | Delete a layer group from a workspace                                 |
| `update_layergroup`            | Update a layer group's details and configuration                      |

### 👥 User & User Group Management

| Tool                 | Description                              |
| :------------------- | :--------------------------------------- |
| `create_user`        | Create a new user for GeoServer security |
| `delete_user`        | Delete a user by name                    |
| `get_all_users`      | List all users in the GeoServer instance |
| `modify_user`        | Modify an existing user's properties     |
| `create_usergroup`   | Create a new user group                  |
| `delete_usergroup`   | Delete a user group                      |
| `get_all_usergroups` | Return all user groups                   |

### 📊 Feature Type & Attribute Management

| Tool                           | Description                                         |
| :----------------------------- | :-------------------------------------------------- |
| `query_features`               | Query features from a vector layer using CQL filter |
| `publish_featurestore`         | Publish an existing featurestore                    |
| `publish_featurestore_sqlview` | Publish a featurestore using a SQL view definition  |
| `edit_featuretype`             | Edit the settings of a feature type in a store      |
| `get_featuretypes`             | List all feature types in a given store             |
| `get_feature_attribute`        | Get feature attribute schema/details                |

### 🎨 Style Management

| Tool                              | Description                                     |
| :-------------------------------- | :---------------------------------------------- |
| `create_style`                    | Create a new SLD style in GeoServer             |
| `publish_style`                   | Assign/publish a style to a layer               |
| `create_catagorized_featurestyle` | Create a categorized style for features         |
| `create_classified_featurestyle`  | Create a classified style for features          |
| `create_coveragestyle`            | Create a raster coverage style                  |
| `create_outline_featurestyle`     | Create a simple outline-only style for features |

### ⚙️ System & Service Operations

| Tool                                      | Description                                                           |
| :---------------------------------------- | :-------------------------------------------------------------------- |
| `get_manifest`                            | Get GeoServer manifest metadata/details                               |
| `get_status`                              | Obtain general server status                                          |
| `get_system_status`                       | Get system status overview/info from GeoServer                        |
| `get_version`                             | Fetch GeoServer version string                                        |
| `reload_geoserver`                        | Reload catalog and config from disk                                   |
| `reset_geoserver`                         | Reset all GeoServer caches/connections                                |
| `update_service`                          | Update selected OGC service options                                   |
| `publish_time_dimension_to_coveragestore` | Add or update a time dimension for a coverage store (for time series) |

### 📝 Style XML Utilities

| Tool                                 | Description                               |
| :----------------------------------- | :---------------------------------------- |
| `style_catagorize_xml`               | Generate SLD for categorized vector style |
| `style_classified_xml`               | Get SLD XML for classified vector style   |
| `style_coverage_style_colormapentry` | Generate color map entries for raster SLD |
| `style_coverage_style_xml`           | Generate XML for raster/coverage SLD      |
| `style_outline_only_xml`             | XML for outline-only style for a geometry |

## 🛠️ Client Development

If you're planning to develop your own client to interact with the GeoServer MCP server, you can find inspiration in the example client implementation at `examples/client.py`. This example demonstrates:

- How to establish a connection with the MCP server
- How to send requests and handle responses
- Basic error handling and connection management
- Example usage of various tools and operations

The example client serves as a good starting point for understanding the protocol and implementing your own client applications.

Also, here is the example usgage:

### List Workspaces

```

Tool: list_workspaces
Parameters: {}
Response: ["default", "demo", "topp", "tiger", "sf"]

```

### Get Layer Information

```

Tool: get_layer_info
Parameters: {
"workspace": "topp",
"layer": "states"
}

```

### Query Features

```

Tool: query_features
Parameters: {
"workspace": "topp",
"layer": "states",
"filter": "PERSONS > 10000000",
"properties": ["STATE_NAME", "PERSONS"]
}

```

### Generate Map

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

## 🔮 Planned Features

- [ ] Coverage and raster data management
- [ ] Security and access control
- [ ] Advanced styling capabilities
- [ ] WPS processing operations
- [ ] GeoWebCache integration

## 🤝 Contributing

We welcome contributions! Here's how you can help:

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

Please ensure your PR description clearly describes the problem and solution. Include the relevant issue number if applicable.

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🔗 Related Projects

- [Model Context Protocol](https://github.com/modelcontextprotocol/python-sdk) - The core MCP implementation
- [GeoServer REST API](https://docs.geoserver.org/latest/en/user/rest/index.html) - Official GeoServer REST documentation
- [GeoServer REST Python Client](https://github.com/gicait/geoserver-rest) - Python client for GeoServer REST API

## 🌐 See Also: GIS MCP

For broader geospatial data automation and even more GIS-related MCP features, see [GIS MCP by mahdin75](https://github.com/mahdin75/gis-mcp).

## 📞 Support

For support, please Open an [issue](https://github.com/mahdin75/geoserver-mcp/issues)

## 🏆 Badges

<div align="center">
  <a href="https://glama.ai/mcp/servers/@mahdin75/geoserver-mcp">
    <img width="380" height="200" src="https://glama.ai/mcp/servers/@mahdin75/geoserver-mcp/badge" alt="GeoServer Server MCP server" />
  </a>
  <br/><br/><br/>
  <a href="https://mcp.so/server/Geoserver%20MCP%20Server/mahdin75">
    <img src="https://mcp.so/logo.png" alt="MCP.so Badge" width="150"/>
  </a>
  <br/><br/><br/>

[![MseeP.ai Security Assessment Badge](https://mseep.net/pr/mahdin75-geoserver-mcp-badge.png)](https://mseep.ai/app/mahdin75-geoserver-mcp)

</div>
