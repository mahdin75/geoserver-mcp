package org.geoservermcp.catalog;

import java.util.LinkedHashMap;
import java.util.Map;

public record LayerView(String name, String workspace, String prefixedName, boolean enabled, String type) {

    public Map<String, Object> toMap() {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("name", name);
        item.put("workspace", workspace);
        item.put("prefixedName", prefixedName);
        item.put("enabled", enabled);
        item.put("type", type);
        return item;
    }
}
