package org.geoservermcp.tools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;
import java.util.Map;
import org.geoservermcp.catalog.CatalogQueryService;
import org.geoservermcp.protocol.McpJson;
import org.junit.jupiter.api.Test;

class ListLayersToolTest {

    @Test
    void schemaMatchesPythonToolNameAndOptionalWorkspace() {
        ListLayersTool tool = new ListLayersTool(mock(CatalogQueryService.class));
        assertEquals("list_layers", tool.name());
        assertTrue(tool.inputSchema().path("properties").has("workspace"));
    }

    @Test
    void blankWorkspaceIsTreatedAsUnfiltered() {
        CatalogQueryService catalog = mock(CatalogQueryService.class);
        when(catalog.listLayers(null)).thenReturn(List.of(Map.of("name", "states")));
        ListLayersTool tool = new ListLayersTool(catalog);
        ObjectNode args = McpJson.mapper().createObjectNode();
        args.put("workspace", "  ");
        Object result = tool.call(args);
        assertEquals(List.of(Map.of("name", "states")), result);
    }
}
