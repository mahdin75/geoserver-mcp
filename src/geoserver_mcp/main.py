"""
GeoServer MCP Server - Main entry point

This module implements an MCP server that connects LLMs to GeoServer REST API,
enabling AI assistants to manage geospatial data and services.
"""

import json
import logging
import os
import sys
import argparse
from typing import Any, Dict, List, Optional, Union

# MCP imports using the new SDK patterns
from mcp.server.fastmcp import FastMCP

# GeoServer REST client
from geo.Geoserver import Geoserver

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s - %(name)s - %(levelname)s - %(message)s",
)
logger = logging.getLogger("geoserver-mcp")

# Create FastMCP instance
mcp = FastMCP("GeoServer MCP")

# Initialize GeoServer connection
def get_geoserver():
    """Get the GeoServer connection using environment variables or command-line arguments."""
    url = os.environ.get("GEOSERVER_URL", "http://localhost:8080/geoserver")
    username = os.environ.get("GEOSERVER_USER", "admin")
    password = os.environ.get("GEOSERVER_PASSWORD", "geoserver")
    
    try:
        geo = Geoserver(url, username=username, password=password)
        logger.info(f"Connected to GeoServer at {url}")
        return geo
    except Exception as e:
        logger.error(f"Failed to connect to GeoServer: {str(e)}")
        return None

# Resource handlers

@mcp.resource("geoserver://catalog/workspaces")
def get_workspaces() -> Dict[str, List[str]]:
    """List available workspaces in GeoServer."""
    geo = get_geoserver()
    if geo is None:
        return {"error": "Not connected to GeoServer"}
    
    try:
        # Use the actual GeoServer REST API to retrieve workspaces
        workspaces = geo.get_workspaces()
        return {"workspaces": workspaces}
    except Exception as e:
        logger.error(f"Error listing workspaces: {str(e)}")
        return {"error": str(e)}

@mcp.resource("geoserver://catalog/layers/{workspace}/{layer}")
def get_layer_info(workspace: str, layer: str) -> Dict[str, Any]:
    """Get information about a specific layer."""
    geo = get_geoserver()
    if geo is None:
        return {"error": "Not connected to GeoServer"}
    
    try:
        # Use the actual GeoServer REST API to get layer information
        layer_info = geo.get_layer(layer, workspace)
        return layer_info
    except Exception as e:
        logger.error(f"Error getting layer info: {str(e)}")
        return {"error": str(e)}

@mcp.resource("geoserver://services/wms/{request}")
def get_wms_resource(request: str) -> Dict[str, Any]:
    """Handle WMS resource requests."""
    geo = get_geoserver()
    if geo is None:
        return {"error": "Not connected to GeoServer"}
    
    try:
        # Use the actual GeoServer REST API to handle WMS requests
        wms_info = geo.get_wms_capabilities()
        return {
            "service": "WMS",
            "request": request,
            "capabilities": wms_info
        }
    except Exception as e:
        logger.error(f"Error handling WMS request: {str(e)}")
        return {"error": str(e)}

@mcp.resource("geoserver://services/wfs/{request}")
def get_wfs_resource(request: str) -> Dict[str, Any]:
    """Handle WFS resource requests."""
    geo = get_geoserver()
    if geo is None:
        return {"error": "Not connected to GeoServer"}
    
    try:
        # Use the actual GeoServer REST API to handle WFS requests
        wfs_info = geo.get_wfs_capabilities()
        return {
            "service": "WFS",
            "request": request,
            "capabilities": wfs_info
        }
    except Exception as e:
        logger.error(f"Error handling WFS request: {str(e)}")
        return {"error": str(e)}

# Tool implementations

@mcp.tool()
def list_workspaces() -> List[str]:
    """List available workspaces in GeoServer."""
    geo = get_geoserver()
    if geo is None:
        raise ValueError("Not connected to GeoServer")
    
    try:
        # Use the actual GeoServer REST API to list workspaces
        workspaces = geo.get_workspaces()
        return workspaces
    except Exception as e:
        logger.error(f"Error listing workspaces: {str(e)}")
        raise ValueError(f"Failed to list workspaces: {str(e)}")

@mcp.tool()
def create_workspace(workspace: str) -> Dict[str, Any]:
    """Create a new workspace in GeoServer.
    
    Args:
        workspace: Name of the workspace to create
    
    Returns:
        Dict with status and result information
    """
    geo = get_geoserver()
    if geo is None:
        raise ValueError("Not connected to GeoServer")
    
    if not workspace:
        raise ValueError("Workspace name is required")
    
    try:
        # Check if workspace already exists
        existing_workspaces = geo.get_workspaces()
        if workspace in existing_workspaces:
            return {
                "status": "info",
                "workspace": workspace,
                "message": f"Workspace '{workspace}' already exists"
            }
        
        # Use the actual GeoServer REST API to create a workspace
        geo.create_workspace(workspace)
        
        return {
            "status": "success",
            "workspace": workspace,
            "message": f"Workspace '{workspace}' created successfully"
        }
    except Exception as e:
        logger.error(f"Error creating workspace: {str(e)}")
        raise ValueError(f"Failed to create workspace: {str(e)}")

@mcp.tool()
def get_layer_info(workspace: str, layer: str) -> Dict[str, Any]:
    """Get detailed information about a layer.
    
    Args:
        workspace: The workspace containing the layer
        layer: The name of the layer
    
    Returns:
        Dict with layer metadata
    """
    geo = get_geoserver()
    if geo is None:
        raise ValueError("Not connected to GeoServer")
    
    if not workspace or not layer:
        raise ValueError("Both workspace and layer name are required")
    
    try:
        # Use the actual GeoServer REST API to get layer information
        layer_info = geo.get_layer(layer, workspace)
        return layer_info
    except Exception as e:
        logger.error(f"Error getting layer info: {str(e)}")
        raise ValueError(f"Failed to get layer info: {str(e)}")

@mcp.tool()
def list_layers(workspace: Optional[str] = None) -> List[Dict[str, Any]]:
    """List layers in GeoServer, optionally filtered by workspace.
    
    Args:
        workspace: Optional workspace to filter layers
    
    Returns:
        List of layer information dictionaries
    """
    geo = get_geoserver()
    if geo is None:
        raise ValueError("Not connected to GeoServer")
    
    try:
        # Use the actual GeoServer REST API to list layers
        if workspace:
            layers = geo.get_layers(workspace)
        else:
            layers = geo.get_layers()
        
        return layers
    except Exception as e:
        logger.error(f"Error listing layers: {str(e)}")
        raise ValueError(f"Failed to list layers: {str(e)}")

@mcp.tool()
def create_layer(workspace: str, layer: str, data_store: str, source: str) -> Dict[str, Any]:
    """Create a new layer in GeoServer.
    
    Args:
        workspace: The workspace for the new layer
        layer: The name of the layer to create
        data_store: The data store to use
        source: The source data (file, table name, etc.)
    
    Returns:
        Dict with status and layer information
    """
    geo = get_geoserver()
    if geo is None:
        raise ValueError("Not connected to GeoServer")
    
    if not workspace or not layer or not data_store:
        raise ValueError("Workspace, layer name, and data store are required")
    
    try:
        # Use the actual GeoServer REST API to create a layer
        geo.create_layer(layer, workspace, data_store, source)
        
        return {
            "status": "success",
            "name": layer,
            "workspace": workspace,
            "data_store": data_store,
            "source": source,
            "message": f"Layer '{layer}' created successfully in workspace '{workspace}'"
        }
    except Exception as e:
        logger.error(f"Error creating layer: {str(e)}")
        raise ValueError(f"Failed to create layer: {str(e)}")

@mcp.tool()
def delete_resource(resource_type: str, workspace: str, name: str) -> Dict[str, Any]:
    """Delete a resource from GeoServer.
    
    Args:
        resource_type: Type of resource to delete (workspace, layer, style, etc.)
        workspace: The workspace containing the resource
        name: The name of the resource
    
    Returns:
        Dict with status and result information
    """
    geo = get_geoserver()
    if geo is None:
        raise ValueError("Not connected to GeoServer")
    
    if not resource_type or not name:
        raise ValueError("Resource type and name are required")
    
    # Validate resource type
    valid_types = ["workspace", "layer", "datastore", "style", "coverage"]
    if resource_type.lower() not in valid_types:
        raise ValueError(f"Invalid resource type. Must be one of: {', '.join(valid_types)}")
    
    try:
        # Use the appropriate GeoServer REST API method based on resource_type
        if resource_type.lower() == "workspace":
            geo.delete_workspace(name)
        elif resource_type.lower() == "layer":
            geo.delete_layer(name, workspace)
        elif resource_type.lower() == "datastore":
            geo.delete_datastore(name, workspace)
        elif resource_type.lower() == "style":
            geo.delete_style(name, workspace)
        elif resource_type.lower() == "coverage":
            geo.delete_coverage(name, workspace)
        
        return {
            "status": "success",
            "type": resource_type,
            "name": name,
            "workspace": workspace if workspace else "global",
            "message": f"{resource_type.capitalize()} '{name}' deleted successfully"
        }
    except Exception as e:
        logger.error(f"Error deleting resource: {str(e)}")
        raise ValueError(f"Failed to delete resource: {str(e)}")

@mcp.tool()
def query_features(
    workspace: str, 
    layer: str, 
    filter: Optional[str] = None,
    properties: Optional[List[str]] = None,
    max_features: Optional[int] = 10
) -> Dict[str, Any]:
    """Query features from a vector layer using CQL filter.
    
    Args:
        workspace: The workspace containing the layer
        layer: The layer to query
        filter: Optional CQL filter expression
        properties: Optional list of properties to return
        max_features: Maximum number of features to return
    
    Returns:
        GeoJSON FeatureCollection with query results
    """
    geo = get_geoserver()
    if geo is None:
        raise ValueError("Not connected to GeoServer")
    
    if not workspace or not layer:
        raise ValueError("Workspace and layer name are required")
    
    try:
        # Construct WFS GetFeature request URL
        url = f"{geo.service_url}/wfs"
        params = {
            "service": "WFS",
            "version": "1.0.0",
            "request": "GetFeature",
            "typeName": f"{workspace}:{layer}",
            "outputFormat": "application/json",
            "maxFeatures": max_features or 10
        }
        
        # Add CQL filter if provided
        if filter:
            params["CQL_FILTER"] = filter
            
        # Add property names if provided
        if properties:
            params["propertyName"] = ",".join(properties)
            
        # Make the request
        import requests
        response = requests.get(url, params=params, auth=(geo.username, geo.password))
        response.raise_for_status()
        
        # Parse the GeoJSON response
        features = response.json()
        
        return {
            "type": "FeatureCollection",
            "features": features.get("features", [])
        }
    except Exception as e:
        logger.error(f"Error querying features: {str(e)}")
        raise ValueError(f"Failed to query features: {str(e)}")

@mcp.tool()
def generate_map(
    layers: List[str],
    styles: Optional[List[str]] = None,
    bbox: Optional[List[float]] = None,
    width: int = 800,
    height: int = 600,
    format: str = "png"
) -> Dict[str, Any]:
    """Generate a map image using WMS GetMap.
    
    Args:
        layers: List of layers to include (format: workspace:layer)
        styles: Optional styles to apply (one per layer)
        bbox: Bounding box [minx, miny, maxx, maxy]
        width: Image width in pixels
        height: Image height in pixels
        format: Image format (png, jpeg, etc.)
    
    Returns:
        Dict with map information and URL
    """
    geo = get_geoserver()
    if geo is None:
        raise ValueError("Not connected to GeoServer")
    
    if not layers:
        raise ValueError("At least one layer must be specified")
    
    # Validate parameters
    if styles and len(styles) != len(layers):
        raise ValueError("Number of styles must match number of layers")
    
    if not bbox:
        bbox = [-180, -90, 180, 90]  # Default to global extent
    
    if len(bbox) != 4:
        raise ValueError("Bounding box must have 4 coordinates: [minx, miny, maxx, maxy]")
    
    # Valid formats
    valid_formats = ["png", "jpeg", "gif", "tiff", "pdf"]
    if format.lower() not in valid_formats:
        raise ValueError(f"Invalid format. Must be one of: {', '.join(valid_formats)}")
    
    try:
        # Construct WMS GetMap URL
        url = f"{geo.service_url}/wms"
        params = {
            "service": "WMS",
            "version": "1.3.0",
            "request": "GetMap",
            "format": f"image/{format}",
            "layers": ",".join(layers),
            "width": width,
            "height": height,
            "crs": "EPSG:4326",
            "bbox": ",".join(map(str, bbox))
        }
        
        # Add styles if provided
        if styles:
            params["styles"] = ",".join(styles)
            
        # Construct the full URL
        import urllib.parse
        query_string = urllib.parse.urlencode(params)
        map_url = f"{url}?{query_string}"
        
        return {
            "url": map_url,
            "width": width,
            "height": height,
            "format": format,
            "layers": layers,
            "styles": styles,
            "bbox": bbox
        }
    except Exception as e:
        logger.error(f"Error generating map: {str(e)}")
        raise ValueError(f"Failed to generate map: {str(e)}")

@mcp.tool()
def create_style(name: str, sld: str, workspace: Optional[str] = None) -> Dict[str, Any]:
    """Create a new SLD style in GeoServer.
    
    Args:
        name: Name for the style
        sld: SLD XML content
        workspace: Optional workspace for the style
    
    Returns:
        Dict with status and style information
    """
    geo = get_geoserver()
    if geo is None:
        raise ValueError("Not connected to GeoServer")
    
    if not name:
        raise ValueError("Style name is required")
    
    if not sld:
        raise ValueError("SLD content is required")
    
    try:
        # Use the actual GeoServer REST API to create a style
        if workspace:
            geo.create_style(name, sld, workspace)
            message = f"Style '{name}' created in workspace '{workspace}'"
        else:
            geo.create_style(name, sld)
            message = f"Global style '{name}' created"
        
        return {
            "status": "success",
            "name": name,
            "workspace": workspace if workspace else "global",
            "message": message
        }
    except Exception as e:
        logger.error(f"Error creating style: {str(e)}")
        raise ValueError(f"Failed to create style: {str(e)}")

def main():
    """Main entry point for the GeoServer MCP server."""
    # Parse command-line arguments
    parser = argparse.ArgumentParser(description="GeoServer MCP Server")
    parser.add_argument("--url", help="GeoServer URL (e.g., http://localhost:8080/geoserver)")
    parser.add_argument("--user", help="GeoServer username")
    parser.add_argument("--password", help="GeoServer password")
    parser.add_argument("--debug", action="store_true", help="Enable debug logging")
    args = parser.parse_args()
    
    # Set environment variables from command-line arguments if provided
    if args.url:
        os.environ["GEOSERVER_URL"] = args.url
    if args.user:
        os.environ["GEOSERVER_USER"] = args.user
    if args.password:
        os.environ["GEOSERVER_PASSWORD"] = args.password
    
    # Set logging level
    if args.debug:
        logging.getLogger().setLevel(logging.DEBUG)
        logger.debug("Debug logging enabled")
    
    try:
        # Start the MCP server
        print("Starting MCP server...")
        print(f"Connecting to GeoServer at {os.environ.get('GEOSERVER_URL', 'http://localhost:8080/geoserver')}")
        mcp.run()
    except KeyboardInterrupt:
        logger.info("Server stopped by user")
    except Exception as e:
        logger.error(f"Server error: {str(e)}")
        sys.exit(1)

if __name__ == "__main__":
    main() 