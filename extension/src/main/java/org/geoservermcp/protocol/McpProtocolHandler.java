package org.geoservermcp.protocol;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoservermcp.config.McpExtensionConfig;
import org.geoservermcp.tools.McpTool;
import org.geoservermcp.tools.McpToolException;
import org.geoservermcp.tools.ToolRegistry;

/**
 * Stateless MCP JSON-RPC handler for Streamable HTTP.
 *
 * <p>Implements the request/response subset used by current remote MCP clients:
 * {@code initialize}, {@code notifications/initialized}, {@code ping}, {@code tools/list},
 * {@code tools/call}. Responses are JSON objects ({@code application/json}), which the Streamable
 * HTTP spec allows instead of an SSE stream.
 */
public class McpProtocolHandler {

    private static final Logger LOGGER = Logger.getLogger(McpProtocolHandler.class.getName());

    private final ToolRegistry tools;
    private final McpExtensionConfig config;

    public McpProtocolHandler(ToolRegistry tools, McpExtensionConfig config) {
        this.tools = tools;
        this.config = config;
    }

    public McpHttpResponse handlePost(String body, String requestedProtocolVersion) {
        String protocolVersion = config.negotiateProtocolVersion(requestedProtocolVersion);
        if (body == null || body.isBlank()) {
            return jsonRpcError(null, JsonRpcErrors.INVALID_REQUEST, "Empty request body", protocolVersion);
        }

        JsonNode message;
        try {
            message = McpJson.mapper().readTree(body);
        } catch (JsonProcessingException e) {
            return jsonRpcError(null, JsonRpcErrors.PARSE_ERROR, "Parse error", protocolVersion);
        }

        if (message.isArray()) {
            return jsonRpcError(
                    null,
                    JsonRpcErrors.INVALID_REQUEST,
                    "JSON-RPC batches are not supported",
                    protocolVersion);
        }
        if (!message.isObject()) {
            return jsonRpcError(null, JsonRpcErrors.INVALID_REQUEST, "Request must be a JSON object", protocolVersion);
        }

        JsonNode jsonrpc = message.get("jsonrpc");
        if (jsonrpc == null || !"2.0".equals(jsonrpc.asText())) {
            return jsonRpcError(
                    message.get("id"), JsonRpcErrors.INVALID_REQUEST, "jsonrpc must be \"2.0\"", protocolVersion);
        }

        JsonNode methodNode = message.get("method");
        if (methodNode == null || !methodNode.isTextual()) {
            return jsonRpcError(message.get("id"), JsonRpcErrors.INVALID_REQUEST, "method is required", protocolVersion);
        }

        String method = methodNode.asText();
        JsonNode id = message.get("id");
        boolean notification = id == null || id.isNull();
        JsonNode params = message.get("params");

        try {
            if (notification) {
                handleNotification(method);
                return McpHttpResponse.accepted(protocolVersion);
            }
            return handleRequest(method, id, params, requestedProtocolVersion);
        } catch (McpToolException e) {
            return jsonRpcError(id, JsonRpcErrors.INVALID_PARAMS, e.getMessage(), protocolVersion);
        } catch (RuntimeException e) {
            LOGGER.log(Level.WARNING, "MCP request failed: " + method, e);
            return jsonRpcError(id, JsonRpcErrors.INTERNAL_ERROR, "Internal error", protocolVersion);
        }
    }

    private void handleNotification(String method) {
        if ("notifications/initialized".equals(method) || "notifications/cancelled".equals(method)) {
            return;
        }
        LOGGER.fine("Ignoring unsupported MCP notification: " + method);
    }

    private McpHttpResponse handleRequest(String method, JsonNode id, JsonNode params, String requestedVersion) {
        String protocolVersion = config.negotiateProtocolVersion(requestedVersion);
        return switch (method) {
            case "initialize" -> initialize(id, params, requestedVersion);
            case "ping" -> success(id, McpJson.mapper().createObjectNode(), protocolVersion);
            case "tools/list" -> toolsList(id, protocolVersion);
            case "tools/call" -> toolsCall(id, params, protocolVersion);
            default -> jsonRpcError(id, JsonRpcErrors.METHOD_NOT_FOUND, "Method not found: " + method, protocolVersion);
        };
    }

    private McpHttpResponse initialize(JsonNode id, JsonNode params, String requestedVersion) {
        String requested = requestedVersion;
        if (params != null && params.hasNonNull("protocolVersion")) {
            requested = params.get("protocolVersion").asText();
        }
        String negotiated = config.negotiateProtocolVersion(requested);

        ObjectNode result = McpJson.mapper().createObjectNode();
        result.put("protocolVersion", negotiated);
        ObjectNode capabilities = result.putObject("capabilities");
        capabilities.putObject("tools");
        ObjectNode serverInfo = result.putObject("serverInfo");
        serverInfo.put("name", config.getServerName());
        serverInfo.put("version", config.getServerVersion());
        result.put(
                "instructions",
                "Read-only GeoServer catalog tools. Visible layers and workspaces follow GeoServer security for the authenticated user. This endpoint does not expose administration, filesystem, or arbitrary REST access.");
        return success(id, result, negotiated);
    }

    private McpHttpResponse toolsList(JsonNode id, String protocolVersion) {
        ObjectNode result = McpJson.mapper().createObjectNode();
        ArrayNode toolsNode = result.putArray("tools");
        for (McpTool tool : tools.list()) {
            ObjectNode item = toolsNode.addObject();
            item.put("name", tool.name());
            item.put("description", tool.description());
            item.set("inputSchema", tool.inputSchema());
        }
        return success(id, result, protocolVersion);
    }

    private McpHttpResponse toolsCall(JsonNode id, JsonNode params, String protocolVersion) {
        if (params == null || !params.isObject() || !params.hasNonNull("name")) {
            return jsonRpcError(id, JsonRpcErrors.INVALID_PARAMS, "tools/call requires params.name", protocolVersion);
        }
        String name = params.get("name").asText();
        McpTool tool = tools.get(name);
        if (tool == null) {
            return toolError(id, "Unknown tool: " + name, protocolVersion);
        }
        JsonNode arguments = params.get("arguments");
        try {
            Object value = tool.call(arguments);
            return toolSuccess(id, value, protocolVersion);
        } catch (McpToolException e) {
            return toolError(id, e.getMessage(), protocolVersion);
        } catch (RuntimeException e) {
            LOGGER.log(Level.WARNING, "Tool failed: " + name, e);
            return toolError(id, "Failed to execute " + name + ": " + e.getMessage(), protocolVersion);
        }
    }

    private McpHttpResponse toolSuccess(JsonNode id, Object value, String protocolVersion) {
        ObjectNode result = McpJson.mapper().createObjectNode();
        ArrayNode content = result.putArray("content");
        ObjectNode text = content.addObject();
        text.put("type", "text");
        try {
            text.put("text", value instanceof String ? (String) value : McpJson.mapper().writeValueAsString(value));
        } catch (JsonProcessingException e) {
            text.put("text", String.valueOf(value));
        }
        result.put("isError", false);
        return success(id, result, protocolVersion);
    }

    private McpHttpResponse toolError(JsonNode id, String message, String protocolVersion) {
        ObjectNode result = McpJson.mapper().createObjectNode();
        ArrayNode content = result.putArray("content");
        ObjectNode text = content.addObject();
        text.put("type", "text");
        text.put("text", message);
        result.put("isError", true);
        return success(id, result, protocolVersion);
    }

    private McpHttpResponse success(JsonNode id, JsonNode result, String protocolVersion) {
        ObjectNode response = McpJson.mapper().createObjectNode();
        response.put("jsonrpc", "2.0");
        copyId(response, id);
        response.set("result", result);
        return McpHttpResponse.json(200, response, protocolVersion);
    }

    private McpHttpResponse jsonRpcError(JsonNode id, int code, String message, String protocolVersion) {
        ObjectNode response = McpJson.mapper().createObjectNode();
        response.put("jsonrpc", "2.0");
        copyId(response, id);
        ObjectNode error = response.putObject("error");
        error.put("code", code);
        error.put("message", message);
        return McpHttpResponse.json(200, response, protocolVersion);
    }

    private static void copyId(ObjectNode response, JsonNode id) {
        if (id == null || id.isNull()) {
            response.putNull("id");
        } else {
            response.set("id", id);
        }
    }
}
