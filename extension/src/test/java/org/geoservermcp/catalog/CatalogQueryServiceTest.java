package org.geoservermcp.catalog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import org.geoservermcp.config.McpExtensionConfig;
import org.geoservermcp.tools.McpToolException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CatalogQueryServiceTest {

    private CatalogAccess catalogAccess;
    private CatalogQueryService service;

    @BeforeEach
    void setUp() {
        catalogAccess = mock(CatalogAccess.class);
        when(catalogAccess.workspaceNames()).thenReturn(List.of("topp", "tiger"));
        when(catalogAccess.workspaceExists("topp")).thenReturn(true);
        when(catalogAccess.workspaceExists("missing")).thenReturn(false);
        when(catalogAccess.layers())
                .thenReturn(List.of(
                        new LayerView("states", "topp", "topp:states", true, "VECTOR"),
                        new LayerView("giant_polygon", "tiger", "tiger:giant_polygon", true, "VECTOR")));
        service = new CatalogQueryService(catalogAccess, new McpExtensionConfig());
    }

    @Test
    void listWorkspacesReturnsNames() {
        assertEquals(List.of("topp", "tiger"), service.listWorkspaces());
    }

    @Test
    void listLayersReturnsAllWhenWorkspaceOmitted() {
        List<Map<String, Object>> layers = service.listLayers(null);
        assertEquals(2, layers.size());
        assertEquals("topp:states", layers.get(0).get("prefixedName"));
        assertEquals("tiger:giant_polygon", layers.get(1).get("prefixedName"));
        assertEquals("VECTOR", layers.get(0).get("type"));
    }

    @Test
    void listLayersFiltersByWorkspace() {
        List<Map<String, Object>> layers = service.listLayers("topp");
        assertEquals(1, layers.size());
        assertEquals("states", layers.get(0).get("name"));
        assertEquals("topp", layers.get(0).get("workspace"));
    }

    @Test
    void listLayersUnknownWorkspaceIsAnError() {
        McpToolException error = assertThrows(McpToolException.class, () -> service.listLayers("missing"));
        assertEquals("Workspace not found: missing", error.getMessage());
    }

    @Test
    void maxListResultsCapsOutput() {
        McpExtensionConfig config = new McpExtensionConfig();
        config.setMaxListResults(1);
        CatalogQueryService limited = new CatalogQueryService(catalogAccess, config);
        assertEquals(1, limited.listWorkspaces().size());
        assertEquals(1, limited.listLayers(null).size());
    }
}
