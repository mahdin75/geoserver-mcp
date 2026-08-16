package org.geoservermcp.protocol;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.Map;
import org.geoservermcp.catalog.CatalogQueryService;
import org.geoservermcp.config.McpExtensionConfig;
import org.geoservermcp.tools.ListLayersTool;
import org.geoservermcp.tools.ListWorkspacesTool;
import org.geoservermcp.tools.ToolRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class McpProtocolHandlerTest {

    private McpProtocolHandler handler;

    @BeforeEach
    void setUp() {
        CatalogQueryService catalog = mock(CatalogQueryService.class);
        when(catalog.listWorkspaces()).thenReturn(List.of("topp"));
        when(catalog.listLayers(isNull()))
                .thenReturn(List.of(Map.of(
                        "name",
                        "states",
                        "workspace",
                        "topp",
                        "prefixedName",
                        "topp:states",
                        "enabled",
                        true,
                        "type",
                        "VECTOR")));
        ToolRegistry registry = new ToolRegistry(List.of(new ListWorkspacesTool(catalog), new ListLayersTool(catalog)));
        handler = new McpProtocolHandler(registry, new McpExtensionConfig());
    }

    @Test
    void initializeNegotiatesProtocolVersionAndAdvertisesTools() throws Exception {
        McpHttpResponse response = handler.handlePost(
                """
                {"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2025-03-26","capabilities":{},"clientInfo":{"name":"test","version":"1.0"}}}
                """,
                "2025-03-26");

        JsonNode body = McpJson.mapper().readTree(response.body());
        assertEquals(200, response.status());
        assertEquals("2025-03-26", body.path("result").path("protocolVersion").asText());
        assertEquals(
                "geoserver-mcp-extension",
                body.path("result").path("serverInfo").path("name").asText());
        assertTrue(body.path("result").path("capabilities").has("tools"));
    }

    @Test
    void initializeFallsBackWhenClientAsksForUnknownVersion() throws Exception {
        McpHttpResponse response = handler.handlePost(
                """
                {"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"1999-01-01","capabilities":{},"clientInfo":{"name":"test","version":"1.0"}}}
                """,
                null);

        JsonNode body = McpJson.mapper().readTree(response.body());
        assertEquals("2025-03-26", body.path("result").path("protocolVersion").asText());
    }

    @Test
    void initializedNotificationReturnsAccepted() {
        McpHttpResponse response = handler.handlePost(
                """
                {"jsonrpc":"2.0","method":"notifications/initialized"}
                """,
                "2025-03-26");

        assertEquals(202, response.status());
        assertFalse(response.hasBody());
    }

    @Test
    void toolsListIncludesReadOnlyCatalogTools() throws Exception {
        McpHttpResponse response = handler.handlePost(
                """
                {"jsonrpc":"2.0","id":2,"method":"tools/list"}
                """,
                "2025-03-26");

        JsonNode tools = McpJson.mapper().readTree(response.body()).path("result").path("tools");
        assertEquals(2, tools.size());
        assertEquals("list_workspaces", tools.get(0).path("name").asText());
        assertEquals("list_layers", tools.get(1).path("name").asText());
        assertEquals("object", tools.get(1).path("inputSchema").path("type").asText());
    }

    @Test
    void listLayersToolReturnsCatalogLayers() throws Exception {
        McpHttpResponse response = handler.handlePost(
                """
                {"jsonrpc":"2.0","id":3,"method":"tools/call","params":{"name":"list_layers","arguments":{}}}
                """,
                "2025-03-26");

        JsonNode body = McpJson.mapper().readTree(response.body());
        assertFalse(body.path("result").path("isError").asBoolean());
        JsonNode layers = McpJson.mapper()
                .readTree(body.path("result").path("content").get(0).path("text").asText());
        assertEquals(1, layers.size());
        assertEquals("states", layers.get(0).path("name").asText());
        assertEquals("topp", layers.get(0).path("workspace").asText());
        assertEquals("topp:states", layers.get(0).path("prefixedName").asText());
    }

    @Test
    void listWorkspacesToolReturnsNames() throws Exception {
        McpHttpResponse response = handler.handlePost(
                """
                {"jsonrpc":"2.0","id":4,"method":"tools/call","params":{"name":"list_workspaces","arguments":{}}}
                """,
                "2025-03-26");

        JsonNode body = McpJson.mapper().readTree(response.body());
        JsonNode workspaces = McpJson.mapper()
                .readTree(body.path("result").path("content").get(0).path("text").asText());
        assertEquals("topp", workspaces.get(0).asText());
    }

    @Test
    void unknownMethodReturnsJsonRpcError() throws Exception {
        McpHttpResponse response = handler.handlePost(
                """
                {"jsonrpc":"2.0","id":5,"method":"nope"}
                """,
                "2025-03-26");

        JsonNode body = McpJson.mapper().readTree(response.body());
        assertEquals(JsonRpcErrors.METHOD_NOT_FOUND, body.path("error").path("code").asInt());
    }

    @Test
    void invalidJsonReturnsParseError() throws Exception {
        McpHttpResponse response = handler.handlePost("{not-json", "2025-03-26");
        JsonNode body = McpJson.mapper().readTree(response.body());
        assertEquals(JsonRpcErrors.PARSE_ERROR, body.path("error").path("code").asInt());
    }

    @Test
    void emptyBodyIsInvalidRequest() throws Exception {
        McpHttpResponse response = handler.handlePost("  ", "2025-03-26");
        JsonNode body = McpJson.mapper().readTree(response.body());
        assertEquals(JsonRpcErrors.INVALID_REQUEST, body.path("error").path("code").asInt());
    }

    @Test
    void unknownToolIsToolErrorNotJsonRpcError() throws Exception {
        McpHttpResponse response = handler.handlePost(
                """
                {"jsonrpc":"2.0","id":6,"method":"tools/call","params":{"name":"delete_everything"}}
                """,
                "2025-03-26");

        JsonNode body = McpJson.mapper().readTree(response.body());
        assertTrue(body.path("result").path("isError").asBoolean());
        assertTrue(body.path("result").path("content").get(0).path("text").asText().contains("Unknown tool"));
        assertTrue(body.path("error").isMissingNode());
    }

    @Test
    void missingToolNameIsInvalidParams() throws Exception {
        McpHttpResponse response = handler.handlePost(
                """
                {"jsonrpc":"2.0","id":7,"method":"tools/call","params":{}}
                """,
                "2025-03-26");

        JsonNode body = McpJson.mapper().readTree(response.body());
        assertEquals(JsonRpcErrors.INVALID_PARAMS, body.path("error").path("code").asInt());
    }

    @Test
    void pingReturnsEmptyResult() throws Exception {
        McpHttpResponse response = handler.handlePost(
                """
                {"jsonrpc":"2.0","id":8,"method":"ping"}
                """,
                "2025-03-26");
        JsonNode body = McpJson.mapper().readTree(response.body());
        assertTrue(body.path("result").isObject());
        assertTrue(body.path("error").isMissingNode());
    }
}
