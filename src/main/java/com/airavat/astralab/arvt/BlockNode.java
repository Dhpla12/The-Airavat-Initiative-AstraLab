package com.airavat.astralab.arvt;

import java.util.LinkedHashMap;
import java.util.Map;

public record BlockNode(String name, Map<String, String> properties) {
    public BlockNode {
        properties = Map.copyOf(new LinkedHashMap<>(properties));
    }
}
