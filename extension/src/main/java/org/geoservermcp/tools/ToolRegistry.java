package org.geoservermcp.tools;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.geoservermcp.catalog.CatalogQueryService;

public class ToolRegistry {

    private final Map<String, McpTool> tools = new LinkedHashMap<>();

    public ToolRegistry(CatalogQueryService catalogQueryService) {
        register(new ListWorkspacesTool(catalogQueryService));
        register(new ListLayersTool(catalogQueryService));
    }

    public ToolRegistry(List<McpTool> tools) {
        for (McpTool tool : tools) {
            register(tool);
        }
    }

    public final void register(McpTool tool) {
        tools.put(tool.name(), tool);
    }

    public McpTool get(String name) {
        return tools.get(name);
    }

    public List<McpTool> list() {
        return new ArrayList<>(tools.values());
    }
}
