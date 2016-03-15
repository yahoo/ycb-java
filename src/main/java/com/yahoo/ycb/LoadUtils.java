/*
 * Copyright 2016 Yahoo inc.
 * Licensed under the terms of the BSD License. Please see LICENSE file in the project home directory for terms.
 */

package com.yahoo.ycb;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.IOException;
import java.util.*;

class LoadUtils {

    protected static List<Dimension> parseDimensions(JsonNode node) throws IOException {
        if (!node.isArray()) {
            throw new IOException("Expecting array.");
        }

        final Iterator<JsonNode> bundles = node.elements();

        if (!bundles.hasNext()) {
            throw new IOException("Expecting array of one element.");
        }

        final JsonNode item = bundles.next();

        if (!item.hasNonNull("dimensions")) {
            throw new IOException("Expecting key \"dimensions\".");
        }
        if (!item.get("dimensions").isArray()) {
            throw new IOException("Expecting \"dimensions\" to be an array");
        }

        final Iterator<JsonNode> dimensions = item.get("dimensions").elements();
        final List<Dimension> results = new ArrayList<>();

        while (dimensions.hasNext()) {
            final JsonNode root = dimensions.next();

            Iterator<Map.Entry<String, JsonNode>> fields = root.fields();
            if (fields.hasNext()) {
                Map.Entry<String, JsonNode> fieldEntry = fields.next();

                results.add(new Dimension(fieldEntry.getKey(), readDimensionValue("*", fieldEntry.getValue())));

            } else {
                throw new IOException("Expecting dimension name");
            }
        }

        return results;
    }


    private static Dimension.DimensionValue readDimensionValue(String value, JsonNode root) {
        final Set<Dimension.DimensionValue> subValues = new HashSet<>();

        if (root.isObject()) {
            final Iterator<Map.Entry<String, JsonNode>> fields = root.fields();
            while (fields.hasNext()) {
                final Map.Entry<String, JsonNode> fieldEntry = fields.next();
                subValues.add(readDimensionValue(fieldEntry.getKey(), fieldEntry.getValue()));

            }
        }
        return new Dimension.DimensionValue(value, subValues);
    }

    protected static Set<Bundle> parseBundles(JsonNode node) throws IOException {
        final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

        if (!node.isArray()) {
            throw new IOException("Expecting array.");
        }

        final Iterator<JsonNode> bundles = node.elements();
        final Set<Bundle> results = new HashSet<>();

        while (bundles.hasNext()) {
            final JsonNode item = bundles.next();
            if (item instanceof ObjectNode) {
                final ObjectNode bundle = (ObjectNode) item;

                if (!bundle.hasNonNull("settings")) {
                    throw new IOException("Expecting \"settings\" key.");
                }

                JsonNode settings = bundle.get("settings");
                Map<String, String> context = new HashMap<>();

                if (settings.isObject()) {
                    context = mapper.treeToValue(settings, Map.class);
                } else if (settings.isArray()) {
                    Iterator<JsonNode> settingElems = settings.elements();
                    while (settingElems.hasNext()) {
                        JsonNode elem = settingElems.next();

                        if (elem.isTextual()) {
                            String v = elem.asText();
                            if ("master".equalsIgnoreCase(v)) {
                                break;
                            } else if (v.contains(":")) {
                                String[] parts = v.split(":", 2);
                                context.put(parts[0], parts[1]);
                            }
                        } else if (elem.isObject()) {
                            context.putAll(mapper.treeToValue(elem, Map.class));
                        }
                    }
                }

                bundle.remove("settings");
                results.add(new Bundle(context, bundle));
            }
        }
        return results;
    }
}
