package org.geoservermcp.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.geoservermcp.protocol.McpJson;

/**
 * One MCP tool. Names, descriptions, and input schemas should stay compatible with the Python
 * GeoServer MCP server where the Java implementation covers the same operation.
 */
public interface McpTool {

    String name();

    String description();

    ObjectNode inputSchema();

    Object call(JsonNode arguments) throws McpToolException;

    /** Write tools are hidden and rejected when {@code mcp.allowWrites} is false. */
    default boolean write() {
        return false;
    }

    default JsonNode missingOrEmpty(JsonNode arguments) {
        return arguments == null || arguments.isNull() ? McpJson.mapper().createObjectNode() : arguments;
    }
}
