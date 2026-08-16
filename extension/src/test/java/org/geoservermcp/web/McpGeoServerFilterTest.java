package org.geoservermcp.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.geoservermcp.catalog.CatalogQueryService;
import org.geoservermcp.config.McpExtensionConfig;
import org.geoservermcp.protocol.McpJson;
import org.geoservermcp.protocol.McpProtocolHandler;
import org.geoservermcp.tools.ListLayersTool;
import org.geoservermcp.tools.ListWorkspacesTool;
import org.geoservermcp.tools.ToolRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

class McpGeoServerFilterTest {

    private McpExtensionConfig config;
    private McpGeoServerFilter filter;

    @BeforeEach
    void setUp() {
        config = new McpExtensionConfig();
        config.setRequireAuthentication(true);
        CatalogQueryService catalog = mock(CatalogQueryService.class);
        when(catalog.listWorkspaces()).thenReturn(List.of("cite"));
        McpProtocolHandler handler = new McpProtocolHandler(
                new ToolRegistry(List.of(new ListWorkspacesTool(catalog), new ListLayersTool(catalog))), config);
        filter = new McpGeoServerFilter(handler, config);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void ignoresNonMcpPaths() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/wms");
        request.setContextPath("/geoserver");
        request.setRequestURI("/geoserver/wms");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertEquals(request, chain.getRequest());
        assertEquals(0, response.getContentLength());
    }

    @Test
    void matchesGeoserverMcpPath() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/mcp");
        request.setContextPath("/geoserver");
        request.setRequestURI("/geoserver/mcp");
        assertTrue(McpGeoServerFilter.isMcpPath(request, "/mcp"));
    }

    @Test
    void getIsMethodNotAllowed() throws Exception {
        MockHttpServletRequest request = postRequest("GET", "");
        MockHttpServletResponse response = new MockHttpServletResponse();
        authenticateAdmin();

        filter.doFilter(request, response, new MockFilterChain());

        assertEquals(405, response.getStatus());
        assertEquals("POST, OPTIONS", response.getHeader("Allow"));
    }

    @Test
    void anonymousIsUnauthorizedWhenAuthRequired() throws Exception {
        SecurityContextHolder.getContext()
                .setAuthentication(new AnonymousAuthenticationToken(
                        "key", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(postRequest("POST", "{}"), response, new MockFilterChain());

        assertEquals(401, response.getStatus());
        assertTrue(response.getHeader("WWW-Authenticate").contains("Basic"));
    }

    @Test
    void authenticatedInitializeSucceeds() throws Exception {
        authenticateAdmin();
        MockHttpServletResponse response = new MockHttpServletResponse();
        String body =
                "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"initialize\",\"params\":{\"protocolVersion\":\"2025-03-26\",\"capabilities\":{},\"clientInfo\":{\"name\":\"t\",\"version\":\"1\"}}}";

        filter.doFilter(postRequest("POST", body), response, new MockFilterChain());

        assertEquals(200, response.getStatus());
        JsonNode json = McpJson.mapper().readTree(response.getContentAsString());
        assertEquals("2.0", json.path("jsonrpc").asText());
        assertEquals("2025-03-26", json.path("result").path("protocolVersion").asText());
        assertEquals("2025-03-26", response.getHeader("MCP-Protocol-Version"));
    }

    @Test
    void disabledExtensionDoesNotIntercept() throws Exception {
        config.setEnabled(false);
        authenticateAdmin();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(postRequest("POST", "{}"), new MockHttpServletResponse(), chain);

        assertNotNull(chain.getRequest());
    }

    @Test
    void optionsReturnsNoContent() throws Exception {
        MockHttpServletRequest request = postRequest("OPTIONS", "");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertEquals(204, response.getStatus());
    }

    private static MockHttpServletRequest postRequest(String method, String body) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, "/mcp");
        request.setContextPath("/geoserver");
        request.setRequestURI("/geoserver/mcp");
        request.setContentType("application/json");
        request.setContent(body.getBytes(StandardCharsets.UTF_8));
        request.addHeader("Accept", "application/json, text/event-stream");
        request.addHeader("MCP-Protocol-Version", "2025-03-26");
        return request;
    }

    private static void authenticateAdmin() {
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(
                        "admin", "geoserver", AuthorityUtils.createAuthorityList("ROLE_ADMINISTRATOR")));
    }
}
