package org.geoservermcp.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.function.Function;

public final class LambdaTool implements McpTool {

    private final String name;
    private final String description;
    private final ObjectNode inputSchema;
    private final Function<JsonNode, Object> handler;
    private final boolean write;

    public LambdaTool(String name, String description, ObjectNode inputSchema, Function<JsonNode, Object> handler) {
        this(name, description, inputSchema, handler, false);
    }

    public LambdaTool(
            String name,
            String description,
            ObjectNode inputSchema,
            Function<JsonNode, Object> handler,
            boolean write) {
        this.name = name;
        this.description = description;
        this.inputSchema = inputSchema;
        this.handler = handler;
        this.write = write;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public String description() {
        return description;
    }

    @Override
    public ObjectNode inputSchema() {
        return inputSchema;
    }

    @Override
    public boolean write() {
        return write;
    }

    @Override
    public Object call(JsonNode arguments) {
        return handler.apply(missingOrEmpty(arguments));
    }
}
