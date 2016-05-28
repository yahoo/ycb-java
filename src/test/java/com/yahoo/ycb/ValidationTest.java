/*
 * Copyright 2016 Yahoo inc.
 * Licensed under the terms of the BSD License. Please see LICENSE file in the project home directory for terms.
 */
package com.yahoo.ycb;

import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;

import static junit.framework.Assert.*;

public class ValidationTest {

    @Test
    public void testIsValid() throws IOException {
        Loader loader = TestUtils.getLoader("example2");

        Configuration configuration = Configuration.load(loader);

        List<ValidationError> errors = configuration.validate();

        assertNotNull(errors);
        assertTrue(errors.isEmpty());
    }

    @Test
    public void testIsNotValid() throws IOException {
        Loader loader = TestUtils.getLoader("example3");

        Configuration configuration = Configuration.load(loader);

        List<ValidationError> errors = configuration.validate();

        assertNotNull(errors);
        assertFalse(errors.isEmpty());

        assertEquals(errors.size(), 2);

        assertEquals(errors.get(0).getReason(), ValidationError.Reason.MISSING_MASTER_PROPERTY);
        assertEquals(errors.get(0).getPath().size(), 2);
        assertEquals(errors.get(0).getPath().get(0), "maestro");
        assertEquals(errors.get(0).getPath().get(1), "enabled_xx");

        assertEquals(errors.get(0).getContextValues().size(), 4);
        assertEquals(errors.get(0).getContextValues().get(0), "*");
        assertEquals(errors.get(0).getContextValues().get(1), "external");
        assertEquals(errors.get(0).getContextValues().get(2), "*");
        assertEquals(errors.get(0).getContextValues().get(3), "production");


        assertEquals(errors.get(1).getReason(), ValidationError.Reason.REPLACING_DIFFERENT_TYPES);
        assertEquals(errors.get(1).getPath().size(), 2);
        assertEquals(errors.get(1).getPath().get(0), "maestro");
        assertEquals(errors.get(1).getPath().get(1), "enable_debug");

        assertEquals(errors.get(1).getContextValues().size(), 4);
        assertEquals(errors.get(1).getContextValues().get(0), "*");
        assertEquals(errors.get(1).getContextValues().get(1), "*");
        assertEquals(errors.get(1).getContextValues().get(2), "*");
        assertEquals(errors.get(1).getContextValues().get(3), "production");
    }
}
