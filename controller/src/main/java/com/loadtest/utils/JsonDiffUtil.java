package com.loadtest.utils;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JsonDiffUtil {

    private static final ObjectMapper mapper = new ObjectMapper();

    public static Map<String, Object> diff(String oldJson, String newJson)
            throws Exception {

        JsonNode oldNode = mapper.readTree(oldJson);
        JsonNode newNode = mapper.readTree(newJson);

        Map<String, Object> changes = new LinkedHashMap<>();
        compare("", oldNode, newNode, changes);
        return changes;
    }

    private static void compare(
            String path,
            JsonNode oldNode,
            JsonNode newNode,
            Map<String, Object> changes
    ) {

        if (!oldNode.equals(newNode)) {
            if (oldNode.isValueNode() && newNode.isValueNode()) {
                changes.put(path, Map.of(
                        "old", oldNode,
                        "new", newNode
                ));
                return;
            }

            Iterator<String> fields = newNode.fieldNames();
            while (fields.hasNext()) {
                String field = fields.next();
                compare(
                        path + "/" + field,
                        oldNode.path(field),
                        newNode.path(field),
                        changes
                );
            }
        }
    }
}
