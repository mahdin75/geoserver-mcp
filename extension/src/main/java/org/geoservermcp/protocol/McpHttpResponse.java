package org.geoservermcp.protocol;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Outcome of handling one HTTP POST to the MCP endpoint.
 *
 * <p>Streamable HTTP (2025-03-26 and later): JSON-RPC requests return {@code application/json};
 * notifications return HTTP 202 with an empty body.
 */
public final class McpHttpResponse {

    private final int status;
    private final String contentType;
    private final String body;
    private final String protocolVersion;

    private McpHttpResponse(int status, String contentType, String body, String protocolVersion) {
        this.status = status;
        this.contentType = contentType;
        this.body = body;
        this.protocolVersion = protocolVersion;
    }

    public static McpHttpResponse json(int status, JsonNode body, String protocolVersion) {
        try {
            return new McpHttpResponse(
                    status, "application/json", McpJson.mapper().writeValueAsString(body), protocolVersion);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize MCP JSON", e);
        }
    }

    public static McpHttpResponse accepted(String protocolVersion) {
        return new McpHttpResponse(202, null, "", protocolVersion);
    }

    public static McpHttpResponse unauthorized(String protocolVersion) {
        ObjectNode error = McpJson.mapper().createObjectNode();
        error.put("error", "Authentication required");
        return json(401, error, protocolVersion);
    }

    public static McpHttpResponse methodNotAllowed(String protocolVersion) {
        ObjectNode error = McpJson.mapper().createObjectNode();
        error.put("error", "Method Not Allowed. Use POST to send MCP JSON-RPC messages.");
        ArrayNode allow = error.putArray("allow");
        allow.add("POST");
        allow.add("OPTIONS");
        return json(405, error, protocolVersion);
    }

    public int status() {
        return status;
    }

    public String contentType() {
        return contentType;
    }

    public String body() {
        return body;
    }

    public String protocolVersion() {
        return protocolVersion;
    }

    public boolean hasBody() {
        return body != null && !body.isEmpty();
    }
}
