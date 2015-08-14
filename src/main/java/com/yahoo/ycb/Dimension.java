/*
 * Copyright 2015 Yahoo inc.
 * Licensed under the terms of the BSD License. Please see LICENSE file in the project home directory for terms.
 */

package com.yahoo.ycb;

import com.google.common.collect.Lists;

import java.util.*;

/**
 * Represents one Dimension in the configuration system: an association
 * between a dimension name, and the tree of values this dimension can
 * be (i.e. the possible values for that this dimension name can assume
 * in contexts).
 */
public class Dimension {

    private final String name;
    private final DimensionValue value;
    private final Map<String, DimensionValue> valueMap = new HashMap<>();

    public Dimension(String name, DimensionValue value) {
        this.name = name;
        this.value = value;
        buildValueMap(value);
    }

    private void buildValueMap(DimensionValue dimValue) {
        valueMap.put(dimValue.value, dimValue);
        for (DimensionValue childDimValue : dimValue.subValues) {
            childDimValue.parent = dimValue;
            buildValueMap(childDimValue);
        }
    }

    /**
     * @param value The dimension value to start from
     * @return A list of ancestries of the value, including itself, until root (*)
     */
    public List<String> getAncestries(String value) {
        final ArrayList<String> result = new ArrayList<>();
        DimensionValue dimValue = valueMap.get(value);

        while (dimValue != null) {
            result.add(dimValue.value);
            dimValue = dimValue.parent;
        }

        return Lists.reverse(result);
    }

    public String getName() {
        return name;
    }

    /**
     * @return deep first traversal of all values
     */
    public List<String> traverse() {
        return value.traverse();
    }

    /**
     * A dimension value is a tree of values, starting with the special "ANY", or "ROOT",
     * or "MASTER" dimension, denoted as "*".
     */
    public static class DimensionValue implements Comparable<DimensionValue> {
        private final String value;
        private final Set<DimensionValue> subValues;
        private DimensionValue parent;

        public DimensionValue(String value, Set<DimensionValue> subValues) {
            this.value = value;
            this.subValues = subValues;
        }

        /**
         * @return deep first traversal of all values
         */
        private List<String> traverse() {
            final ArrayList<String> values = new ArrayList<>();
            values.add(value);
            subValues.stream().sorted().forEach(dimensionValue -> values.addAll(dimensionValue.traverse()));
            return values;
        }

        @Override
        public int compareTo(DimensionValue o) {
            return value.compareTo(o.value);
        }
    }
}
