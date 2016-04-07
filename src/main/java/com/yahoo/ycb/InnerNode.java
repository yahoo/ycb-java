/*
 * Copyright 2015 Yahoo inc.
 * Licensed under the terms of the BSD License. Please see LICENSE file in the project home directory for terms.
 */

package com.yahoo.ycb;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class InnerNode extends LookupTree {

    private Dimension dimension;
    private final Map<String, LookupTree> edges = new HashMap<>();

    /**
     * @return A list of leaf children of this Node
     */
    @Override
    protected List<PathLeaf> traverse() {
        return edges.entrySet().stream()
                .flatMap(entry -> entry.getValue().traverse().stream()
                        .map(pathLeaf -> new PathLeaf(pathLeaf, entry.getKey())))
                .collect(Collectors.toList());
    }

    @Override
    public JsonNode project(Map<String, String> context, String[] path) {
        JsonNode delta = NullNode.getInstance();

        for (String value : dimension.getAncestries(context.getOrDefault(dimension.getName(), ANY_VALUE))) {
            final LookupTree child = edges.get(value);

            if (child != null) {
                delta = mergeDelta(delta, child.project(context, path));
            }
        }

        return delta;
    }

    @Override
    protected void insert(List<Dimension> dimensions, Map<String, String> context, JsonNode delta) {
        dimension = dimensions.get(0);

        String contextValue = context.getOrDefault(dimension.getName(), ANY_VALUE);

        LookupTree child = edges.get(contextValue);
        if (child == null) {
            child = dimensions.size() == 1 ? new LeafNode() : new InnerNode();
            edges.put(contextValue, child);
        }

        child.insert(dimensions.subList(1, dimensions.size()), context, delta);
    }
}

