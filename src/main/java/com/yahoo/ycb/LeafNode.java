/*
 * Copyright 2015 Yahoo inc.
 * Licensed under the terms of the BSD License. Please see LICENSE file in the project home directory for terms.
 */

package com.yahoo.ycb;


import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;
import java.util.Map;

class LeafNode extends LookupTree {

    private JsonNode delta;

    @Override
    public JsonNode project(Map<String, String> context, String[] path) {
        JsonNode current = delta;
        for (String part : path) {
            if (current == null) {
                break;
            }
            current = current.get(part);
        }
        return current;
    }

    @Override
    protected void insert(List<Dimension> dimensions, Map<String, String> context, JsonNode delta) {
        assert dimensions.isEmpty();

        this.delta = mergeDelta(this.delta, delta);
    }

}
