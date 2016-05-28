/*
 * Copyright 2015 Yahoo inc.
 * Licensed under the terms of the BSD License. Please see LICENSE file in the project home directory for terms.
 */

package com.yahoo.ycb;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
        validateBundles(dimensions, bundles);

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

    private static void validateBundles(final List<Dimension> dimensions, final Set<Bundle> bundles) {
        final Map<String, List<String>> dimensionValues = dimensions.stream()
            .collect(Collectors.toMap(Dimension::getName, Dimension::traverse));

        bundles.forEach(bundle ->
            bundle.getContext().forEach((dimension, value) -> {
                if (!dimensionValues.containsKey(dimension)) {
                    throw new IllegalArgumentException("Unknown dimension: " + dimension);
                }
                if (!dimensionValues.get(dimension).contains(value)) {
                    throw new IllegalArgumentException("Invalid value for dimension: " + dimension + " -> " + value);
                }
            })
        );
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


    static JsonNode mergeDelta(JsonNode delta1, JsonNode delta2) throws ValidationException  {
        return mergeDelta(delta1, delta2, false);
    }

    static JsonNode mergeDelta(JsonNode delta1, JsonNode delta2, boolean strictMode) throws ValidationException  {
        return mergeDelta(delta1, delta2, strictMode, Collections.emptyList());
    }

    /**
     * @param delta1 Json 1
     * @param delta2 Json 2
     * @param strictMode If true, will throw an exception if a key exists in Json 2 but not Json 1
     * @return Merged Json
     */
    static JsonNode mergeDelta(JsonNode delta1, JsonNode delta2, boolean strictMode, List<String> path) throws ValidationException {
        if (delta2 == null || delta2.isNull()) {
            return delta1;
        } else if (delta1 != null && delta1.isObject() && delta2.isObject()) {
            // delta 1 and delta 2 are objects

            final ObjectNode result = new ObjectNode(JsonNodeFactory.instance);
            for (final Iterator<Map.Entry<String, JsonNode>> fields = delta1.fields(); fields.hasNext();) {
                final Map.Entry<String, JsonNode> field = fields.next();

                if (delta2.has(field.getKey())) {
                    final List<String> newPath = new ArrayList<>();
                    newPath.addAll(path);
                    newPath.add(field.getKey());

                    result.set(field.getKey(), mergeDelta(field.getValue(), delta2.get(field.getKey()), strictMode, newPath));
                } else {
                    result.set(field.getKey(), field.getValue());
                }
            }
            for (final Iterator<Map.Entry<String, JsonNode>> fields = delta2.fields(); fields.hasNext();) {
                final Map.Entry<String, JsonNode> field = fields.next();

                if (!result.has(field.getKey())) {
                    final List<String> newPath = new ArrayList<>();
                    newPath.addAll(path);
                    newPath.add(field.getKey());

                    if (strictMode) {
                        throw new ValidationException(newPath, ValidationError.Reason.MISSING_MASTER_PROPERTY);
                    }

                    result.set(field.getKey(), field.getValue());
                }
            }

            return result;
        } else {
            // delta 1 and delta 2 are not objects

            if (strictMode && delta1 != null && !delta2.isNull() && delta1.getClass() != delta2.getClass()) {
                // delta 1 and delta 2 classes are different, and delta2 is not json null (json null is allowed to inhabit any type)
                throw new ValidationException(path, ValidationError.Reason.REPLACING_DIFFERENT_TYPES);
            }

            return delta2;
        }
    }

    /**
     * @return A list of leaf children of this Node
     */
    protected abstract List<PathLeaf> traverse();

    public abstract JsonNode project(Map<String, String> context, String[] path);

    protected abstract void insert(List<Dimension> dimensions, Map<String, String> context, JsonNode delta);

    public List<ValidationError> validate() {
        final List<PathLeaf> pathLeafs = traverse();

        // find the "Master" delta
        final JsonNode masterDelta = pathLeafs.stream()
                .filter(p -> p.contextValues.stream().allMatch(v -> v.equals(ANY_VALUE)))
                .map(PathLeaf::getDelta)
                .findFirst()
                .orElse(NullNode.getInstance());

        // iterate over all "non-root" deltas
        return pathLeafs.stream()
                .filter(p -> p.contextValues.stream().anyMatch(v -> !v.equals(ANY_VALUE)))
                .flatMap(p -> {
                    try {
                        mergeDelta(masterDelta, p.getDelta(), true);
                        return Stream.empty();
                    } catch (ValidationException e) {
                        return Stream.of( new ValidationError(e.getPath(), e.getReason(), p.getContextValues()) );
                    }
                })
                .collect(Collectors.toList());
    }

    protected static class ValidationException extends RuntimeException {
        private final List<String> path;
        private final ValidationError.Reason reason;


        public ValidationException(List<String> path, ValidationError.Reason reason) {
            this.path = path;
            this.reason = reason;
        }

        public List<String> getPath() {
            return path;
        }

        public ValidationError.Reason getReason() {
            return reason;
        }
    }

    /**
     * Represents a key from the root to the leaf
     */
    protected static class PathLeaf {
        private final List<String> contextValues;
        private final JsonNode delta;

        public PathLeaf(JsonNode delta) {
            contextValues = Collections.emptyList();
            this.delta = delta;
        }

        public PathLeaf(PathLeaf child, String contextValue) {
            contextValues = new ArrayList<>();
            contextValues.addAll(child.getContextValues());
            contextValues.add(contextValue);

            delta = child.delta;
        }

        public List<String> getContextValues() {
            return contextValues;
        }

        public JsonNode getDelta() {
            return delta;
        }
    }

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
