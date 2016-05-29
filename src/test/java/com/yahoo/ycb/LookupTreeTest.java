/*
 * Copyright 2016 Yahoo inc.
 * Licensed under the terms of the BSD License. Please see LICENSE file in the project home directory for terms.
 */

package com.yahoo.ycb;

import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URL;

public class LookupTreeTest {

    @Test
    public void testUnknownDimension() throws IOException {
        Loader loader = TestUtils.getLoader("unknownDimension");

        try {
            Configuration.load(loader);
            Assert.fail("Expecting IllegalArgumentException to be thrown for an unknown dimension");
        }
        catch (IllegalArgumentException e) {}
    }

    @Test
    public void testInvalidValue() throws IOException {
        Loader loader = TestUtils.getLoader("invalidDimension");

        try {
            Configuration.load(loader);
            Assert.fail("Expecting IllegalArgumentException to be thrown for an invalid dimension value");
        }
        catch (IllegalArgumentException e) {}
    }

}
