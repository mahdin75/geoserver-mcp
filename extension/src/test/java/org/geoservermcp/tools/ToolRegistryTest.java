package org.geoservermcp.tools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import org.geoservermcp.catalog.CatalogQueryService;
import org.junit.jupiter.api.Test;

class ToolRegistryTest {

    @Test
    void registersPythonCompatibleCatalogTools() {
        ToolRegistry registry = new ToolRegistry(mock(CatalogQueryService.class));

        assertFalse(registry.get("list_workspaces").write());
        assertFalse(registry.get("list_layers").write());
        assertFalse(registry.get("get_layer_info").write());
        assertFalse(registry.get("query_features").write());
        assertFalse(registry.get("generate_map").write());
        assertFalse(registry.get("get_datastores").write());
        assertFalse(registry.get("get_version").write());

        assertTrue(registry.get("create_workspace").write());
        assertTrue(registry.get("create_layer").write());
        assertTrue(registry.get("delete_resource").write());
        assertTrue(registry.get("create_featurestore").write());
        assertTrue(registry.get("publish_style").write());

        assertEquals("create_workspace", registry.get("create_workspace").name());
        assertTrue(registry.get("query_features").inputSchema().path("properties").has("max_features"));
        assertNotNull(registry.get("edit_featuretype"));
        assertEquals(30, registry.list().size());
    }
}
