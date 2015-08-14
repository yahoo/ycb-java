/*
 * Copyright 2015 Yahoo inc.
 * Licensed under the terms of the BSD License. Please see LICENSE file in the project home directory for terms.
 */

package com.yahoo.ycb;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Map;

/**
 * A Bundle is an association between a Context and a Delta.
 * <p>
 * A Context is a map between Dimension Names, and Dimension values -
 * specifying in which situation the Delta will be applied.
 * <p>
 * A Delta is a piece of arbitrary unstructured configuration.
 */
public class Bundle {

    private final Map<String, String> context;
    private final JsonNode delta;

    public Bundle(Map<String, String> context, JsonNode delta) {
        this.context = context;
        this.delta = delta;
    }

    public Map<String, String> getContext() {
        return context;
    }

    public JsonNode getDelta() {
        return delta;
    }
}
