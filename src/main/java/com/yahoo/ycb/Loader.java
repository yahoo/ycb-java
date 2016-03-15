/*
 * Copyright 2015 Yahoo inc.
 * Licensed under the terms of the BSD License. Please see LICENSE file in the project home directory for terms.
 */

package com.yahoo.ycb;

import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.util.*;

/**
 * A Loader is a Configuration Source.
 * <p>
 * A Configuration comprises of two parts: dimensions, which specify how contexts should be structured; and
 * Bundles, which in turns are associations of contexts with a configuration Delta.
 */
public interface Loader {

    List<Dimension> getDimensions() throws IOException;

    Set<Bundle> getBundles() throws IOException;

}
