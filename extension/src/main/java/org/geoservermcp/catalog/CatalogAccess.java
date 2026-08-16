package org.geoservermcp.catalog;

import java.util.List;

/** Read-only catalog view used by MCP tools. Keeps GeoServer types out of the protocol layer. */
public interface CatalogAccess {

    List<String> workspaceNames();

    boolean workspaceExists(String name);

    List<LayerView> layers();
}
