package org.geoservermcp.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;
import java.util.Map;
import org.geoservermcp.catalog.CatalogQueryService;
import org.geoservermcp.protocol.McpJson;

/**
 * Compatible with the Python {@code list_layers} tool: optional {@code workspace} filter, list of
 * layer objects.
 */
public class ListLayersTool implements McpTool {

    public static final String NAME = "list_layers";

    private final CatalogQueryService catalogQueryService;

    public ListLayersTool(CatalogQueryService catalogQueryService) {
        this.catalogQueryService = catalogQueryService;
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public String description() {
        return "List layers in GeoServer, optionally filtered by workspace.";
    }

    @Override
    public ObjectNode inputSchema() {
        ObjectNode schema = McpJson.mapper().createObjectNode();
        schema.put("type", "object");
        ObjectNode properties = schema.putObject("properties");
        ObjectNode workspace = properties.putObject("workspace");
        workspace.put("type", "string");
        workspace.put("description", "Optional workspace to filter layers");
        schema.putArray("required");
        schema.put("additionalProperties", false);
        return schema;
    }

    @Override
    public Object call(JsonNode arguments) {
        JsonNode args = missingOrEmpty(arguments);
        if (args.has("workspace") && !args.get("workspace").isNull() && !args.get("workspace").isTextual()) {
            throw new McpToolException("workspace must be a string");
        }
        String workspace = args.path("workspace").isMissingNode() || args.path("workspace").isNull()
                ? null
                : args.path("workspace").asText();
        if (workspace != null && workspace.isBlank()) {
            workspace = null;
        }
        List<Map<String, Object>> layers = catalogQueryService.listLayers(workspace);
        return layers;
    }
}
