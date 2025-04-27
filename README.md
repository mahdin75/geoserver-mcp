# GeoServer MCP Server

<p align="center">
  <img src="https://badge.mcpx.dev?type=server" title="MCP Server"/>
A Model Context Protocol (MCP) server implementation that connects Large Language Models (LLMs) to the GeoServer REST API, enabling AI assistants to interact with geospatial data and services.

</p>

<div align="center">
  <img src="docs/images/geoserver-mcp.png" alt="GeoServer MCP Server Logo" width="400"/>
</div>

> ![Alpha](https://img.shields.io/badge/Version-0.1.0--Alpha-green)
>
> Version 0.2.0 (Alpha) is under active development and will be released shortly. We are open to contributions and welcome developers to join us in building this project.

## 🎥 Demo

<div align="center">
  <img src="demo/list_workspaces.png" alt="GeoServer MCP Server Demo" width="400"/>
</div>

## 📋 Table of Contents

- [Features](#-features)
- [Prerequisites](#-prerequisites)
- [Installation](#️-installation)
  <!-- - [Docker Installation](#️-installation-docker) -->
  - [pip Installation](#️-installation-pip)
  - [Development Installation](#️-development-installation)
- [Available Tools](#️-available-tools)
  - [Workspace and Layer Management](#️-workspace-and-layer-management)
  - [Data Operations](#️-data-operations)
  - [Visualization](#️-visualization)
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

## 📋 Prerequisites

- Python 3.10 or higher
- Running GeoServer instance with REST API enabled
- MCP-compatible client (like Claude Desktop or Cursor)
- Internet connection for package installation

## 🛠️ Installation

Choose the installation method that best suits your needs:

<!-- ### 🛠️ Installation (Docker)

The Docker installation is the quickest and most isolated way to run the GeoServer MCP server. It's ideal for:

- Quick testing and evaluation
- Production deployments
- Environments where you want to avoid Python dependencies
- Consistent deployment across different systems

1. Run geoserver-mcp:

```bash
docker pull geoserver-mcp
docker run -d geoserver-mcp
```

2. Configure the clients:

If you are using Claude Desktop, edit `claude_desktop_config.json`
If you are using Cursor, Create `.cursor/mcp.json`

```json
{
  "mcpServers": {
    "geoserver-mcp": {
      "command": "docker",
      "args": ["run", "-i", "--rm", "geoserver-mcp"],
      "env": [
        "GEOSERVER_URL",
        "http://localhost:8080/geoserver",
        "GEOSERVER_USER",
        "admin",
        "GEOSERVER_PASSWORD",
        "geoserver"
      ]
    }
  }
}
``` -->

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

## 🛠️ Available Tools

### 🛠️ Workspace and Layer Management

| Tool               | Description                 |
| ------------------ | --------------------------- |
| `list_workspaces`  | Get available workspaces    |
| `create_workspace` | Create a new workspace      |
| `get_layer_info`   | Get detailed layer metadata |
| `list_layers`      | List layers in a workspace  |
| `create_layer`     | Create a new layer          |
| `delete_resource`  | Remove resources            |

### 🛠️ Data Operations

| Tool              | Description                        |
| ----------------- | ---------------------------------- |
| `query_features`  | Execute CQL queries on vector data |
| `update_features` | Modify feature attributes          |
| `delete_features` | Remove features based on criteria  |

### 🛠️ Visualization

| Tool           | Description                     |
| -------------- | ------------------------------- |
| `generate_map` | Create styled map images        |
| `create_style` | Define new SLD styles           |
| `apply_style`  | Apply existing styles to layers |

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
</div>
