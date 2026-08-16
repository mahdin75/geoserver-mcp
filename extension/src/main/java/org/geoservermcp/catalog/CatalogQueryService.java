package org.geoservermcp.catalog;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.geoservermcp.config.McpExtensionConfig;
import org.geoservermcp.tools.McpToolException;

/** Read-only catalog queries with result caps. */
public class CatalogQueryService {

    private final CatalogAccess catalogAccess;
    private final McpExtensionConfig config;

    public CatalogQueryService(CatalogAccess catalogAccess, McpExtensionConfig config) {
        this.catalogAccess = Objects.requireNonNull(catalogAccess, "catalogAccess");
        this.config = Objects.requireNonNull(config, "config");
    }

    public List<String> listWorkspaces() {
        List<String> names = new ArrayList<>();
        for (String name : catalogAccess.workspaceNames()) {
            if (name != null) {
                names.add(name);
                if (names.size() >= config.getMaxListResults()) {
                    break;
                }
            }
        }
        return names;
    }

    public List<Map<String, Object>> listLayers(String workspace) {
        String filter = workspace == null || workspace.isBlank() ? null : workspace.trim();
        if (filter != null && !catalogAccess.workspaceExists(filter)) {
            throw new McpToolException("Workspace not found: " + filter);
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (LayerView layer : catalogAccess.layers()) {
            if (filter != null && !filter.equals(layer.workspace())) {
                continue;
            }
            result.add(layer.toMap());
            if (result.size() >= config.getMaxListResults()) {
                break;
            }
        }
        return result;
    }
}
