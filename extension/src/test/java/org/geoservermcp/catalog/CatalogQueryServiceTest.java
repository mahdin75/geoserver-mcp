package org.geoservermcp.catalog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
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

    @Test
    void generateMapBuildsWmsUrl() {
        when(catalogAccess.serviceBaseUrl()).thenReturn("http://localhost:8080/geoserver");
        Map<String, Object> map = service.generateMap(List.of("topp:states"), null, null, 800, 600, "png");
        String url = (String) map.get("url");
        assertTrue(url.startsWith("http://localhost:8080/geoserver/wms?"));
        assertTrue(url.contains("request=GetMap"));
        assertTrue(url.contains("layers=topp%3Astates") || url.contains("layers=topp:states"));
        assertEquals(List.of(-180d, -90d, 180d, 90d), map.get("bbox"));
    }

    @Test
    void queryFeaturesCapsMaxFeatures() {
        when(catalogAccess.queryFeatures("topp", "states", null, null, 100))
                .thenReturn(Map.of("type", "FeatureCollection", "features", List.of()));
        service.queryFeatures("topp", "states", null, null, 5000);
        org.mockito.Mockito.verify(catalogAccess).queryFeatures("topp", "states", null, null, 100);
    }

    @Test
    void writesAreRejectedWhenDisabled() {
        McpExtensionConfig config = new McpExtensionConfig();
        config.setAllowWrites(false);
        CatalogQueryService locked = new CatalogQueryService(catalogAccess, config);
        McpToolException error = assertThrows(McpToolException.class, () -> locked.createWorkspace("demo"));
        assertTrue(error.getMessage().contains("Write tools are disabled"));
    }
}
