package org.geoservermcp.web;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.geoserver.filters.GeoServerFilter;
import org.geoserver.platform.ExtensionPriority;
import org.geoservermcp.config.McpExtensionConfig;
import org.geoservermcp.protocol.McpHttpResponse;
import org.geoservermcp.protocol.McpProtocolHandler;
import org.geoservermcp.security.McpSecurity;

/**
 * Intercepts {@code /mcp} before GeoServer's OWS dispatcher. Registered automatically because
 * {@link org.geoserver.filters.SpringDelegatingFilter} loads every {@link GeoServerFilter} bean.
 *
 * <p>The resulting public URL is {@code https://host/geoserver/mcp} when GeoServer is deployed at
 * {@code /geoserver}.
 */
public class McpGeoServerFilter implements GeoServerFilter, ExtensionPriority {

    static final String PROTOCOL_VERSION_HEADER = "MCP-Protocol-Version";

    private final McpProtocolHandler protocolHandler;
    private final McpExtensionConfig config;

    public McpGeoServerFilter(McpProtocolHandler protocolHandler, McpExtensionConfig config) {
        this.protocolHandler = protocolHandler;
        this.config = config;
    }

    @Override
    public void init(FilterConfig filterConfig) {
        // no-op: configuration comes from Spring
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (!(request instanceof HttpServletRequest httpRequest)
                || !(response instanceof HttpServletResponse httpResponse)) {
            chain.doFilter(request, response);
            return;
        }
        if (!config.isEnabled() || !isMcpPath(httpRequest, config.getPath())) {
            chain.doFilter(request, response);
            return;
        }
        handleMcp(httpRequest, httpResponse);
    }

    void handleMcp(HttpServletRequest request, HttpServletResponse response) throws IOException {
        applyCors(request, response);
        String protocolHeader = request.getHeader(PROTOCOL_VERSION_HEADER);
        String method = request.getMethod() == null ? "GET" : request.getMethod().toUpperCase(Locale.ROOT);

        if ("OPTIONS".equals(method)) {
            response.setStatus(HttpServletResponse.SC_NO_CONTENT);
            response.setHeader("Allow", "POST, OPTIONS");
            return;
        }

        if (!"POST".equals(method)) {
            write(response, McpHttpResponse.methodNotAllowed(config.negotiateProtocolVersion(protocolHeader)));
            return;
        }

        if (config.isRequireAuthentication() && !McpSecurity.isAuthenticated()) {
            response.setHeader("WWW-Authenticate", "Basic realm=\"GeoServer MCP\"");
            write(response, McpHttpResponse.unauthorized(config.negotiateProtocolVersion(protocolHeader)));
            return;
        }

        String body = readBody(request);
        McpHttpResponse mcpResponse = protocolHandler.handlePost(body, protocolHeader);
        write(response, mcpResponse);
    }

    private static String readBody(HttpServletRequest request) throws IOException {
        try (InputStream in = request.getInputStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    static boolean isMcpPath(HttpServletRequest request, String configuredPath) {
        String uri = request.getRequestURI();
        if (uri == null) {
            return false;
        }
        String context = request.getContextPath() == null ? "" : request.getContextPath();
        String path = uri.substring(Math.min(context.length(), uri.length()));
        if (path.isEmpty()) {
            path = "/";
        }
        String mcp = McpExtensionConfig.normalizePath(configuredPath);
        return path.equals(mcp) || path.equals(mcp + "/");
    }

    private void applyCors(HttpServletRequest request, HttpServletResponse response) {
        if (!config.isCorsEnabled()) {
            return;
        }
        String origin = request.getHeader("Origin");
        String allowOrigin = config.getCorsAllowOrigin();
        if ("*".equals(allowOrigin)) {
            response.setHeader("Access-Control-Allow-Origin", origin != null ? origin : "*");
        } else if (origin != null && allowOrigin.equals(origin)) {
            response.setHeader("Access-Control-Allow-Origin", origin);
        }
        response.setHeader("Access-Control-Allow-Methods", "POST, OPTIONS");
        response.setHeader(
                "Access-Control-Allow-Headers", "Content-Type, Accept, Authorization, MCP-Protocol-Version");
        response.setHeader("Access-Control-Max-Age", "3600");
        response.setHeader("Vary", "Origin");
    }

    private static void write(HttpServletResponse response, McpHttpResponse mcpResponse) throws IOException {
        response.setStatus(mcpResponse.status());
        if (mcpResponse.protocolVersion() != null) {
            response.setHeader(PROTOCOL_VERSION_HEADER, mcpResponse.protocolVersion());
        }
        if (mcpResponse.status() == HttpServletResponse.SC_METHOD_NOT_ALLOWED) {
            response.setHeader("Allow", "POST, OPTIONS");
        }
        if (mcpResponse.hasBody()) {
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.setContentType(mcpResponse.contentType());
            byte[] bytes = mcpResponse.body().getBytes(StandardCharsets.UTF_8);
            response.setContentLength(bytes.length);
            response.getOutputStream().write(bytes);
        } else {
            response.setContentLength(0);
        }
    }

    @Override
    public void destroy() {
        // no-op
    }

    @Override
    public int getPriority() {
        return 10;
    }
}
