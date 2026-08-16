package org.geoservermcp.tools;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class JsonArgs {

    private JsonArgs() {}

    public static String requireText(JsonNode args, String field) {
        JsonNode node = args.get(field);
        if (node == null || node.isNull() || !node.isTextual() || node.asText().isBlank()) {
            throw new McpToolException(field + " is required");
        }
        return node.asText().trim();
    }

    public static String optionalText(JsonNode args, String field) {
        JsonNode node = args.get(field);
        if (node == null || node.isNull()) {
            return null;
        }
        if (!node.isTextual()) {
            throw new McpToolException(field + " must be a string");
        }
        String value = node.asText().trim();
        return value.isEmpty() ? null : value;
    }

    public static int optionalInt(JsonNode args, String field, int fallback) {
        JsonNode node = args.get(field);
        if (node == null || node.isNull()) {
            return fallback;
        }
        if (!node.isNumber() && !node.isTextual()) {
            throw new McpToolException(field + " must be a number");
        }
        return node.asInt(fallback);
    }

    public static List<String> requireStringList(JsonNode args, String field) {
        List<String> values = optionalStringList(args, field);
        if (values == null || values.isEmpty()) {
            throw new McpToolException(field + " is required");
        }
        return values;
    }

    public static List<String> optionalStringList(JsonNode args, String field) {
        JsonNode node = args.get(field);
        if (node == null || node.isNull()) {
            return null;
        }
        if (!node.isArray()) {
            throw new McpToolException(field + " must be an array of strings");
        }
        List<String> values = new ArrayList<>();
        for (JsonNode item : node) {
            values.add(item.asText());
        }
        return values;
    }

    public static List<Double> optionalDoubleList(JsonNode args, String field) {
        JsonNode node = args.get(field);
        if (node == null || node.isNull()) {
            return null;
        }
        if (!node.isArray()) {
            throw new McpToolException(field + " must be an array of numbers");
        }
        List<Double> values = new ArrayList<>();
        for (JsonNode item : node) {
            values.add(item.asDouble());
        }
        return values;
    }

    public static Map<String, String> optionalStringMap(JsonNode args, String field) {
        JsonNode node = args.get(field);
        if (node == null || node.isNull()) {
            return Map.of();
        }
        if (!node.isObject()) {
            throw new McpToolException(field + " must be an object");
        }
        Map<String, String> values = new LinkedHashMap<>();
        Iterator<String> names = node.fieldNames();
        while (names.hasNext()) {
            String name = names.next();
            JsonNode value = node.get(name);
            values.put(name, value == null || value.isNull() ? null : value.asText());
        }
        return values;
    }
}
