package org.geoservermcp.tools;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.geoservermcp.protocol.McpJson;

final class Schemas {

    private Schemas() {}

    static ObjectNode object(String... required) {
        ObjectNode schema = McpJson.mapper().createObjectNode();
        schema.put("type", "object");
        schema.putObject("properties");
        var requiredNode = schema.putArray("required");
        for (String field : required) {
            requiredNode.add(field);
        }
        schema.put("additionalProperties", false);
        return schema;
    }

    static ObjectNode prop(ObjectNode schema, String name, String type, String description) {
        ObjectNode field = schema.with("properties").putObject(name);
        field.put("type", type);
        field.put("description", description);
        return schema;
    }

    static ObjectNode arrayProp(ObjectNode schema, String name, String itemType, String description) {
        ObjectNode field = schema.with("properties").putObject(name);
        field.put("type", "array");
        field.put("description", description);
        field.putObject("items").put("type", itemType);
        return schema;
    }

    static ObjectNode objectProp(ObjectNode schema, String name, String description) {
        ObjectNode field = schema.with("properties").putObject(name);
        field.put("type", "object");
        field.put("description", description);
        field.put("additionalProperties", true);
        return schema;
    }
}
