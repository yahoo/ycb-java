/*
 * Copyright 2015 Yahoo inc.
 * Licensed under the terms of the BSD License. Please see LICENSE file in the project home directory for terms.
 */

package com.yahoo.ycb;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

abstract class LookupTree {

    /**
     * The implicit value of the root of every dimension, representing the "master" or any match.
     */
    public static final String ANY_VALUE = "*";

    public static LookupTree create(Loader loader, Map<String, String> fixedContext) throws IOException {
        return create(loader.getDimensions(), loader.getBundles(), fixedContext);
    }

    /**
     * @param dimensions   The list of dimensions
     * @param bundles      The list of configuration bundles (associations of contexts with deltas)
     * @param fixedContext fixed context, i.e. specify a subset of context that all projections will adhere.
     * @return The Lookup Tree
     */
    public static LookupTree create(final List<Dimension> dimensions, Set<Bundle> bundles, final Map<String, String> fixedContext) {
        // drop dimensions present in the fixed context (so we have a shallower tree).
        final List<Dimension> actualDimensions = new ArrayList<>();
        dimensions.forEach(
                dimension -> {
                    if (!fixedContext.containsKey(dimension.getName())) {
                        actualDimensions.add(dimension);
                    }
                });

        // if the dimensions are empty, create a leaf node
        final LookupTree node = actualDimensions.isEmpty() ? new LeafNode() : new InnerNode();

        bundles.stream()
                // make sure we are inserting in the correct order (more generic first, more specific after).
                // this is specially important if we have fixed Context
                .sorted(new BundleComparator(dimensions))

                        // only insert bundles which are compatible with fixed context
                .filter(bundle -> fixedContextMatch(dimensions, fixedContext, bundle.getContext()))
                .forEach(bundle -> {
                    // insert (with drop dimensions) the bundle in the tree node
                    node.insert(actualDimensions, bundle.getContext(), bundle.getDelta());
                });

        return node;
    }

    private static boolean fixedContextMatch(List<Dimension> dimensions, final Map<String, String> fixedContext, Map<String, String> bundleContext) {
        final Map<String, List<String>> dimensionAncestries = dimensions.stream()
                .filter(dimension -> fixedContext.containsKey(dimension.getName()))
                .collect(Collectors.toMap(Dimension::getName,
                        dimension -> dimension.getAncestries(fixedContext.get(dimension.getName()))
                ));

        return bundleContext.entrySet().stream()
                .filter(entry -> dimensionAncestries.containsKey(entry.getKey()))
                .allMatch(entry -> dimensionAncestries.get(entry.getKey()).contains(entry.getValue()));
    }

    static JsonNode mergeDelta(JsonNode delta1, JsonNode delta2) {
        if (delta2 == null || delta2.isNull()) {
            return delta1;
        } else if (delta1 != null && delta1.isObject() && delta2.isObject()) {
            final ObjectNode result = new ObjectNode(JsonNodeFactory.instance);
            Iterator<Map.Entry<String, JsonNode>> fields = delta1.fields();
            while (fields.hasNext()) {
                final Map.Entry<String, JsonNode> field = fields.next();

                if (delta2.has(field.getKey())) {
                    result.set(field.getKey(), mergeDelta(field.getValue(), delta2.get(field.getKey())));
                } else {
                    result.set(field.getKey(), field.getValue());
                }
            }
            fields = delta2.fields();
            while (fields.hasNext()) {
                final Map.Entry<String, JsonNode> field = fields.next();

                if (!result.has(field.getKey())) {
                    result.set(field.getKey(), field.getValue());
                }
            }

            return result;
        } else {
            return delta2;
        }
    }

    public abstract JsonNode project(Map<String, String> context, String[] path);

    protected abstract void insert(List<Dimension> dimensions, Map<String, String> context, JsonNode delta);

    /**
     * Compare two contexts (given dimensions) based on which is more generic (lower) and more specific (bigger).
     */
    private static class BundleComparator implements Comparator<Bundle> {
        private final List<Dimension> dimensions;

        public BundleComparator(List<Dimension> dimensions) {
            this.dimensions = dimensions;
        }

        @Override
        public int compare(Bundle b1, Bundle b2) {
            final Map<String, String> c1 = b1.getContext();
            final Map<String, String> c2 = b2.getContext();

            for (Dimension dimension : dimensions) {
                final String v1 = c1.getOrDefault(dimension.getName(), ANY_VALUE);
                final String v2 = c2.getOrDefault(dimension.getName(), ANY_VALUE);

                if (!v1.equals(v2)) {

                    if (dimension.getAncestries(v1).contains(v2)) {
                        // b1 is more specific (bigger) than v2
                        return 1;
                    } else if (dimension.getAncestries(v2).contains(v1)) {
                        // b1 is more generic (less) than v2
                        return -1;
                    }
                    // else they are in different branch
                }
            }
            return 0;
        }
    }
}
