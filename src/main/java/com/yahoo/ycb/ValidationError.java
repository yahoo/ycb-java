/*
 * Copyright 2016 Yahoo inc.
 * Licensed under the terms of the BSD License. Please see LICENSE file in the project home directory for terms.
 */
package com.yahoo.ycb;


import java.util.List;

public class ValidationError {
    private final List<String> path;
    private final Reason reason;
    private final List<String> contextValues;

    public ValidationError(List<String> path, Reason reason, List<String> contextValues) {
        this.path = path;
        this.reason = reason;
        this.contextValues = contextValues;
    }

    public List<String> getPath() {
        return path;
    }

    public Reason getReason() {
        return reason;
    }

    public List<String> getContextValues() {
        return contextValues;
    }

    @Override
    public String toString() {
        return "context=(" + String.join(", ", contextValues) + "), path=" + String.join(".", path) + ": " + reason;
    }

    public enum Reason {
        MISSING_MASTER_PROPERTY,
        REPLACING_DIFFERENT_TYPES;

        @Override
        public String toString() {
            switch (this) {
                case MISSING_MASTER_PROPERTY: return "missing property in master";
                case REPLACING_DIFFERENT_TYPES: return "property being replaced with different type";
            }
            return "invalid";
        }
    }
}
