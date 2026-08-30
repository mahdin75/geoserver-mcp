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
def list_workspaces() -> Dict[str, List[str]]:
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

# ============================
# Datastore/Coveragestore Tools
# ============================

@mcp.tool()
def create_datastore(workspace: str, name: str, params: dict) -> dict:
    """Create a new datastore in the given workspace.
    
    Args:
        workspace (str): Name of the workspace to add the datastore to.
        name (str): The datastore name.
        params (dict): Connection parameters (host, dbtype, etc.).
    
    Returns:
        dict: Details of the created datastore or API response.
    """
    geo = get_geoserver()
    if geo is None:
        raise ValueError("Not connected to GeoServer")
    return geo.create_datastore(name, workspace, **params)

@mcp.tool()
def create_featurestore(workspace: str, name: str, params: dict) -> dict:
    """Create a new featurestore in the given workspace.
    
    Args:
        workspace (str): Target workspace.
        name (str): Featurestore name.
        params (dict): Connection/configuration parameters.
    
    Returns:
        dict: Details of the created featurestore.
    """
    geo = get_geoserver()
    if geo is None:
        raise ValueError("Not connected to GeoServer")
    return geo.create_featurestore(name, workspace, **params)

@mcp.tool()
def create_gpkg_datastore(workspace: str, name: str, file_path: str) -> dict:
    """Create a GeoPackage (GPKG) datastore.
    
    Args:
        workspace (str): Workspace name.
        name (str): Datastore name.
        file_path (str): Path to the .gpkg file (relative to --storage if provided)
    Returns:
        dict: Creation result.
    """
    geo = get_geoserver()
    if geo is None:
        raise ValueError("Not connected to GeoServer")
    file_path = resolve_storage_path(file_path)
    return geo.create_gpkg_datastore(workspace, name, file_path)

@mcp.tool()
def create_shp_datastore(workspace: str, name: str, file_path: str) -> dict:
    """Create an ESRI Shapefile datastore.
    
    Args:
        workspace (str): Workspace in GeoServer.
        name (str): New datastore name.
        file_path (str): Path to .shp or zipped shapefile (relative to --storage if provided)
    Returns:
        dict: Creation result.
    """
    geo = get_geoserver()
    if geo is None:
        raise ValueError("Not connected to GeoServer")
    file_path = resolve_storage_path(file_path)
    return geo.create_shp_datastore(workspace, name, file_path)

@mcp.tool()
def create_coveragestore(workspace: str, name: str, params: dict) -> dict:
    """Create a new coveragestore in a workspace.
    
    Args:
        workspace (str): Workspace name.
        name (str): Coveragestore name.
        params (dict): Parameters (url, type, etc.)
    
    Returns:
        dict: Creation response.
    """
    geo = get_geoserver()
    if geo is None:
        raise ValueError("Not connected to GeoServer")
    return geo.create_coveragestore(workspace, name, **params)

@mcp.tool()
def delete_coveragestore(workspace: str, name: str) -> dict:
    """Delete a coveragestore from a workspace.
    
    Args:
        workspace (str): Workspace name.
        name (str): Coveragestore name.
    Returns:
        dict: Response from API about deletion result.
    """
    geo = get_geoserver()
    if geo is None:
        raise ValueError("Not connected to GeoServer")
    return geo.delete_coveragestore(name, workspace)

@mcp.tool()
def get_coveragestore(workspace: str, name: str) -> dict:
    """Get details about a single coveragestore.
    
    Args:
        workspace (str): Workspace.
        name (str): Coveragestore.
    Returns:
        dict: Metadata for the coveragestore.
    """
    geo = get_geoserver()
    if geo is None:
        raise ValueError("Not connected to GeoServer")
    return geo.get_coveragestore(name, workspace)

@mcp.tool()
def get_coveragestores(workspace: str) -> list:
    """Get all coveragestores in a workspace.
    
    Args:
        workspace (str): Name of the workspace.
    Returns:
        list: List of coveragestores.
    """
    geo = get_geoserver()
    if geo is None:
        raise ValueError("Not connected to GeoServer")
    return geo.get_coveragestores(workspace)

@mcp.tool()
def get_datastore(workspace: str, name: str) -> dict:
    """Get a specific datastore by name.
    
    Args:
        workspace (str): Workspace name.
        name (str): Datastore name.
    Returns:
        dict: Datastore description.
    """
    geo = get_geoserver()
    if geo is None:
        raise ValueError("Not connected to GeoServer")
    return geo.get_datastore(name, workspace)

@mcp.tool()
def get_datastores(workspace: str) -> list:
    """List all datastores in the given workspace.
    
    Args:
        workspace (str): Workspace.
    Returns:
        list: List of datastores for the workspace.
    """
    geo = get_geoserver()
    if geo is None:
        raise ValueError("Not connected to GeoServer")
    return geo.get_datastores(workspace)

# ============================
# Layer Group Tools (DOCSTRINGS)
# ============================

@mcp.tool()
def create_layergroup(workspace: str, name: str, layers: list, styles: list = None) -> dict:
    """Create a new layer group with specific layers and (optionally) styles.
    
    Args:
        workspace (str): The workspace for the group.
        name (str): Name of the layer group.
        layers (list): List of layers to include.
        styles (list, optional): List of styles for layers.
    Returns:
        dict: Creation result.
    """
    geo = get_geoserver()
    if geo is None:
        raise ValueError("Not connected to GeoServer")
    return geo.create_layergroup(workspace, name, layers, styles)

@mcp.tool()
def get_layergroup(workspace: str, name: str) -> dict:
    """Get a layer group from a workspace.
    
    Args:
        workspace (str): Workspace to search.
        name (str): Name of the group.
    Returns:
        dict: Layer group metadata/details.
    """
    geo = get_geoserver()
    if geo is None:
        raise ValueError("Not connected to GeoServer")
    return geo.get_layergroup(name, workspace)

@mcp.tool()
def get_layergroups(workspace: str) -> list:
    """List all layer groups in a workspace.
    
    Args:
        workspace (str): Workspace.
    Returns:
        list: Layer group names or info dicts.
    """
    geo = get_geoserver()
    if geo is None:
        raise ValueError("Not connected to GeoServer")
    return geo.get_layergroups(workspace)

@mcp.tool()
def add_layer_to_layergroup(layer_name: str, layer_workspace: str, layergroup_name: str, layergroup_workspace: str = None) -> dict:
    """Add a specific layer to a layer group.
    
    Args:
        layer_name (str): Layer to add.
        layer_workspace (str): Workspace for the layer.
        layergroup_name (str): Target group name.
        layergroup_workspace (str, optional): Workspace for the group.
    Returns:
        dict: Status and any info.
    """
    geo = get_geoserver()
    if geo is None:
        raise ValueError("Not connected to GeoServer")
    return geo.add_layer_to_layergroup(layer_name, layer_workspace, layergroup_name, layergroup_workspace)

@mcp.tool()
def remove_layer_from_layergroup(layer_name: str, layer_workspace: str, layergroup_name: str, layergroup_workspace: str = None) -> dict:
    """Remove a layer from a group.
    
    Args:
        layer_name (str): Layer.
        layer_workspace (str): Layer workspace.
        layergroup_name (str): Group.
        layergroup_workspace (str, optional): Group workspace.
    Returns:
        dict: Deletion result.
    """
    geo = get_geoserver()
    if geo is None:
        raise ValueError("Not connected to GeoServer")
    return geo.remove_layer_from_layergroup(layer_name, layer_workspace, layergroup_name, layergroup_workspace)

@mcp.tool()
def delete_layergroup(workspace: str, name: str) -> dict:
    """Delete a layer group from a workspace.
    
    Args:
        workspace (str): Workspace.
        name (str): Group to delete.
    Returns:
        dict: Result of the delete operation.
    """
    geo = get_geoserver()
    if geo is None:
        raise ValueError("Not connected to GeoServer")
    return geo.delete_layergroup(name, workspace)

@mcp.tool()
def update_layergroup(layergroup_name: str, title: str = None, abstract_text: str = None, formats: str = 'html', metadata: list = None, keywords: list = None) -> dict:
    """Update a layer group's details and configuration.
    
    Args:
        layergroup_name (str): The group to update.
        title (str, optional): New title.
        abstract_text (str, optional): Abstract/description.
        formats (str, optional): Response format (default html).
        metadata (list, optional): Extra metadata entries.
        keywords (list, optional): Associated keywords.
    Returns:
        dict: Result message.
    """
    geo = get_geoserver()
    if geo is None:
        raise ValueError("Not connected to GeoServer")
    return geo.update_layergroup(
        layergroup_name, title, abstract_text, formats, metadata or [], keywords or [])

# ============================
# User & User Group Tools (DOCSTRINGS)
# ============================

@mcp.tool()
def create_user(username: str, password: str, enabled: bool = True, properties: dict = None) -> dict:
    """Create a new user for GeoServer security.

    Args:
        username (str): New user's name.
        password (str): Password for the user.
        enabled (bool): Whether account is enabled on creation.
        properties (dict, optional): Additional user properties.

    Returns:
        dict: API response on user creation.
    """
    geo = get_geoserver()
    if geo is None:
        raise ValueError("Not connected to GeoServer")
    return geo.create_user(username, password, enabled=enabled, properties=properties or {})

@mcp.tool()
def delete_user(username: str) -> dict:
    """Delete a user by name.
    
    Args:
        username (str): User to remove.
    Returns:
        dict: Deletion result.
    """
    geo = get_geoserver()
    if geo is None:
        raise ValueError("Not connected to GeoServer")
    return geo.delete_user(username)

@mcp.tool()
def get_all_users() -> list:
    """List all users in the GeoServer instance.
    
    Returns:
        list: Usernames or user info dicts.
    """
    geo = get_geoserver()
    if geo is None:
        raise ValueError("Not connected to GeoServer")
    return geo.get_all_users()

@mcp.tool()
def modify_user(username: str, **kwargs) -> dict:
    """Modify an existing user's properties (password, enabled state, properties, etc).
    
    Args:
        username (str): Name to update.
        **kwargs: Attributes to set/update.
    Returns:
        dict: Response message.
    """
    geo = get_geoserver()
    if geo is None:
        raise ValueError("Not connected to GeoServer")
    return geo.modify_user(username, **kwargs)

@mcp.tool()
def create_usergroup(name: str, users: list = None) -> dict:
    """Create a new user group.
    
    Args:
        name (str): Name for the user group.
        users (list, optional): Users to add initially.
    Returns:
        dict: Creation outcome.
    """
    geo = get_geoserver()
    if geo is None:
        raise ValueError("Not connected to GeoServer")
    return geo.create_usergroup(name, users or [])

@mcp.tool()
def delete_usergroup(name: str) -> dict:
    """Delete a user group.
    
    Args:
        name (str): Name of group to remove.
    Returns:
        dict: Result of group deletion.
    """
    geo = get_geoserver()
    if geo is None:
        raise ValueError("Not connected to GeoServer")
    return geo.delete_usergroup(name)

@mcp.tool()
def get_all_usergroups() -> list:
    """Return all user groups.
    
    Returns:
        list: Names or info dicts for all groups.
    """
    geo = get_geoserver()
    if geo is None:
        raise ValueError("Not connected to GeoServer")
    return geo.get_all_usergroups()

# ============================
# Featurestore, Advanced, Style Tools (DOCSTRINGS)
# ============================

@mcp.tool()
def publish_featurestore(workspace: str, store_name: str, params: dict) -> dict:
    """Publish an existing featurestore.

    Args:
        workspace (str): Target workspace.
        store_name (str): Featurestore name.
        params (dict): Publication settings (table, type, etc).
    Returns:
        dict: API result.
    """
    geo = get_geoserver()
    if geo is None:
        raise ValueError("Not connected to GeoServer")
    return geo.publish_featurestore(store_name, workspace, **params)

@mcp.tool()
def publish_featurestore_sqlview(workspace: str, store_name: str, params: dict, sqlview_params: list) -> dict:
    """Publish a featurestore using a SQL view definition.
    
    Args:
        workspace (str): Workspace context.
        store_name (str): Featurestore.
        params (dict): Featurestore details.
        sqlview_params (list): SQL view parameter dicts.
    Returns:
        dict: Publication outcome.
    """
    geo = get_geoserver()
    if geo is None:
        raise ValueError("Not connected to GeoServer")
    return geo.publish_featurestore_sqlview(store_name, workspace, params, sqlview_params)

@mcp.tool()
def edit_featuretype(workspace: str, store_name: str, featuretype: str, **kwargs) -> dict:
    """Edit the settings of a feature type in a store.
    
    Args:
        workspace (str): Workspace containing store.
        store_name (str): Store name.
        featuretype (str): Feature type to modify.
        **kwargs: Any updatable featuretype attributes.
    Returns:
        dict: API update result.
    """
    geo = get_geoserver()
    if geo is None:
        raise ValueError("Not connected to GeoServer")
    return geo.edit_featuretype(featuretype, store_name, workspace, **kwargs)

@mcp.tool()
def get_featuretypes(workspace: str, store_name: str) -> list:
    """List all feature types in a given store.
    
    Args:
        workspace (str): Workspace.
        store_name (str): Store name.
    Returns:
        list: Featuretype metadata/list.
    """
    geo = get_geoserver()
    if geo is None:
        raise ValueError("Not connected to GeoServer")
    return geo.get_featuretypes(store_name, workspace)

@mcp.tool()
def get_feature_attribute(workspace: str, store_name: str, featuretype: str) -> dict:
    """Get feature attribute schema/details.
    
    Args:
        workspace (str): Workspace.
        store_name (str): Store containing layer.
        featuretype (str): The layer or featuretype name.
    Returns:
        dict: Attribute info.
    """
    geo = get_geoserver()
    if geo is None:
        raise ValueError("Not connected to GeoServer")
    return geo.get_feature_attribute(featuretype, store_name, workspace)

@mcp.tool()
def get_manifest() -> dict:
    """Get GeoServer manifest metadata/details.
    
    Returns:
        dict: Manifest metadata from GeoServer.
    """
    geo = get_geoserver()
    if geo is None:
        raise ValueError("Not connected to GeoServer")
    return geo.get_manifest()

@mcp.tool()
def get_status() -> dict:
    """Obtain general server status.
    
    Returns:
        dict: Status/health report.
    """
    geo = get_geoserver()
    if geo is None:
        raise ValueError("Not connected to GeoServer")
    return geo.get_status()

@mcp.tool()
def get_system_status() -> dict:
    """Get system status overview/info from GeoServer.
    
    Returns:
        dict: System info (CPU, memory, etc).
    """
    geo = get_geoserver()
    if geo is None:
        raise ValueError("Not connected to GeoServer")
    return geo.get_system_status()

@mcp.tool()
def get_version() -> str:
    """Fetch GeoServer version string.
    
    Returns:
        str: GeoServer version.
    """
    geo = get_geoserver()
    if geo is None:
        raise ValueError("Not connected to GeoServer")
    return geo.get_version()

@mcp.tool()
def reload_geoserver() -> str:
    """Reload catalog and config from disk.
    
    Returns:
        str: Result message from reload.
    """
    geo = get_geoserver()
    if geo is None:
        raise ValueError("Not connected to GeoServer")
    return geo.reload()

@mcp.tool()
def reset_geoserver() -> str:
    """Reset all GeoServer caches/connections.
    
    Returns:
        str: Result message from reset operation.
    """
    geo = get_geoserver()
    if geo is None:
        raise ValueError("Not connected to GeoServer")
    return geo.reset()

@mcp.tool()
def update_service(service: str, options: dict) -> str:
    """Update selected OGC service options.
    
    Args:
        service (str): Service (e.g., 'wfs', 'wms').
        options (dict): Options/params to change.
    Returns:
        str: Result message from service update.
    """
    geo = get_geoserver()
    if geo is None:
        raise ValueError("Not connected to GeoServer")
    return geo.update_service(service, **options)

@mcp.tool()
def publish_time_dimension_to_coveragestore(store_name: str = None, workspace: str = None, presentation: str = 'LIST', units: str = 'ISO8601', default_value: str = 'MINIMUM', content_type: str = 'application/xml; charset=UTF-8') -> dict:
    """Add or update a time dimension for a coverage store (for time series).

    Args:
        store_name (str, optional): Target coverage store.
        workspace (str, optional): Workspace.
        presentation (str, optional): Presentation style.
        units (str, optional): Time units.
        default_value (str, optional): Default value for the time dimension.
        content_type (str, optional): Payload content type.

    Returns:
        dict: Response from GeoServer.
    """
    geo = get_geoserver()
    if geo is None:
        raise ValueError("Not connected to GeoServer")
    return geo.publish_time_dimension_to_coveragestore(store_name, workspace, presentation, units, default_value, content_type)

@mcp.tool()
def publish_style(layer_name: str, style_name: str, workspace: str) -> dict:
    """Assign/publish a style to a layer.

    Args:
        layer_name (str): The target layer.
        style_name (str): The style to apply.
        workspace (str): Workspace context.
    Returns:
        dict: API response from publish.
    """
    geo = get_geoserver()
    if geo is None:
        raise ValueError("Not connected to GeoServer")
    return geo.publish_style(layer_name, style_name, workspace)

@mcp.tool()
def create_catagorized_featurestyle(style_name: str, column_name: str, column_distinct_values, workspace: str = None, color_ramp: str = 'tab20', geom_type: str = 'polygon') -> dict:
    """Create a categorized style for features (polygon, line, point) using column values.

    Args:
        style_name (str): New style name.
        column_name (str): Column to base categories on.
        column_distinct_values: Values (categories) to style.
        workspace (str, optional): Workspace for the style.
        color_ramp (str, optional): Color ramp.
        geom_type (str, optional): Geometry type.
    Returns:
        dict: API response.
    """
    geo = get_geoserver()
    if geo is None:
        raise ValueError("Not connected to GeoServer")
    return geo.create_catagorized_featurestyle(style_name, column_name, column_distinct_values, workspace, color_ramp, geom_type)

@mcp.tool()
def create_classified_featurestyle(style_name: str, column_name: str, column_distinct_values, workspace: str = None, color_ramp: str = 'tab20', geom_type: str = 'polygon') -> dict:
    """Create a classified style for features using distinct column values/classes.

    Args:
        style_name (str): New style name.
        column_name (str): Which column.
        column_distinct_values: Classes/categories for styling.
        workspace (str, optional): Workspace.
        color_ramp (str, optional): Ramp.
        geom_type (str, optional): Geometry type.
    Returns:
        dict: API response.
    """
    geo = get_geoserver()
    if geo is None:
        raise ValueError("Not connected to GeoServer")
    return geo.create_classified_featurestyle(style_name, column_name, column_distinct_values, workspace, color_ramp, geom_type)

@mcp.tool()
def create_coveragestyle(style_name: str, params: dict) -> dict:
    """Create a raster coverage style (colormap, ...)

    Args:
        style_name (str): Name for the style.
        params (dict): Colormap/settings for style.
    Returns:
        dict: Server response.
    """
    geo = get_geoserver()
    if geo is None:
        raise ValueError("Not connected to GeoServer")
    return geo.create_coveragestyle(style_name, **params)

@mcp.tool()
def create_outline_featurestyle(style_name: str, outline_color: str, workspace: str = None) -> dict:
    """Create a simple outline-only style for features.

    Args:
        style_name (str): Style name.
        outline_color (str): HEX outline color.
        workspace (str, optional): Workspace.
    Returns:
        dict: API creation response.
    """
    geo = get_geoserver()
    if geo is None:
        raise ValueError("Not connected to GeoServer")
    return geo.create_outline_featurestyle(style_name, outline_color, workspace)

# Style module functions
from geo import Style

@mcp.tool()
def style_catagorize_xml(column_name: str, values: list, color_ramp: str = None, geom_type: str = 'polygon') -> str:
    """Generate SLD for categorized vector style (SVG fill/block display).
    Args:
        column_name (str): Column for category.
        values (list): Category values.
        color_ramp (str, optional): Color ramp.
        geom_type (str, optional): Geometry.
    Returns:
        str: SLD XML.
    """
    return Style.catagorize_xml(column_name, values, color_ramp, geom_type)

@mcp.tool()
def style_classified_xml(style_name: str, column_name: str, values: list, color_ramp: str = None, geom_type: str = 'polygon') -> str:
    """Get SLD XML for classified vector style.
    Args:
        style_name (str): Name of style.
        column_name (str): Column to use for class.
        values (list): Scalar classes.
        color_ramp (str, optional): Colors.
        geom_type (str, optional): Geometry type.
    Returns:
        str: SLD XML.
    """
    return Style.classified_xml(style_name, column_name, values, color_ramp, geom_type)

@mcp.tool()
def style_coverage_style_colormapentry(color_ramp, min_value: float, max_value: float, number_of_classes: int = None):
    """Generate color map entries for raster SLD.
    Args:
        color_ramp: List/dict of colors.
        min_value (float): Minimum value.
        max_value (float): Maximum value.
        number_of_classes (int, optional): Number of classes.
    Returns:
        List or dict: Colormap structure for SLD.
    """
    return Style.coverage_style_colormapentry(color_ramp, min_value, max_value, number_of_classes)

@mcp.tool()
def style_coverage_style_xml(color_ramp, style_name, cmap_type, min_value, max_value, number_of_classes, opacity):
    """Generate XML for raster/coverage SLD.
    Args:
        color_ramp: Color ramp (list, dict...)
        style_name: Name of style.
        cmap_type: Colormap type.
        min_value: Minimum.
        max_value: Maximum.
        number_of_classes: Number of classes.
        opacity: Opacity.
    Returns:
        str: XML for the style.
    """
    return Style.coverage_style_xml(color_ramp, style_name, cmap_type, min_value, max_value, number_of_classes, opacity)

@mcp.tool()
def style_outline_only_xml(color: str, width: float, geom_type: str = 'polygon') -> str:
    """XML for outline-only style for a geometry.
    Args:
        color (str): Outline color (hex).
        width (float): Line width.
        geom_type (str): Geometry type ('polygon', etc).
    Returns:
        str: SLD XML for outline style.
    """
    return Style.outline_only_xml(color, width, geom_type)

def resolve_storage_path(path):
    """Return absolute file path considering storage root if set."""
    base = os.environ.get("GEOSERVER_STORAGE_PATH", "")
    if not path:
        return path
    if os.path.isabs(path) or not base:
        return path
    return os.path.join(base, path)

def main():
    """Main entry point for the GeoServer MCP server."""
    # Parse command-line arguments
    parser = argparse.ArgumentParser(description="GeoServer MCP Server")
    parser.add_argument("--url", help="GeoServer URL (e.g., http://localhost:8080/geoserver)")
    parser.add_argument("--user", help="GeoServer username")
    parser.add_argument("--password", help="GeoServer password")
    parser.add_argument("--debug", action="store_true", help="Enable debug logging")
    parser.add_argument("--storage", help="Base path for file read/write operations (e.g. D:/data or /srv/geoserver-mcp/files)")
    args = parser.parse_args()
    
    # Set environment variables from command-line arguments if provided
    if args.url:
        os.environ["GEOSERVER_URL"] = args.url
    if args.user:
        os.environ["GEOSERVER_USER"] = args.user
    if args.password:
        os.environ["GEOSERVER_PASSWORD"] = args.password
    if args.storage:
        os.environ["GEOSERVER_STORAGE_PATH"] = args.storage
    
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
