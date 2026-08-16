package org.geoservermcp.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;
import org.geoservermcp.catalog.CatalogQueryService;
import org.geoservermcp.protocol.McpJson;

/**
 * Compatible with the Python {@code list_workspaces} tool: no input, list of workspace name
 * strings.
 */
public class ListWorkspacesTool implements McpTool {

    public static final String NAME = "list_workspaces";

    private final CatalogQueryService catalogQueryService;

    public ListWorkspacesTool(CatalogQueryService catalogQueryService) {
        this.catalogQueryService = catalogQueryService;
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public String description() {
        return "List available workspaces in GeoServer.";
    }

    @Override
    public ObjectNode inputSchema() {
        ObjectNode schema = McpJson.mapper().createObjectNode();
        schema.put("type", "object");
        schema.putObject("properties");
        schema.putArray("required");
        schema.put("additionalProperties", false);
        return schema;
    }

    @Override
    public Object call(JsonNode arguments) {
        List<String> workspaces = catalogQueryService.listWorkspaces();
        return workspaces;
    }
}
