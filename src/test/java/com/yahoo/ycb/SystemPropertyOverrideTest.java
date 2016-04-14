/*
 * Copyright 2016 Yahoo inc.
 * Licensed under the terms of the BSD License. Please see LICENSE file in the project home directory for terms.
 */
package com.yahoo.ycb;

import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static junit.framework.Assert.*;
import static org.junit.Assert.assertEquals;

public class SystemPropertyOverrideTest {


    private static Loader getLoader(String name) {
        final URL url = Thread.currentThread().getContextClassLoader().getResource(name);
        assert url != null;

        return new FileSystemLoader(new File(url.getPath()));
    }

    @Before
    public void setUp() throws Exception {
        System.clearProperty("routes.main_route.method");
        System.clearProperty("service_x.api_config.endpoint");
    }

    @Test
    public void testDontOverride() throws IOException {
        Loader loader = getLoader("example1");

        Map<String, String> fixedContext = new HashMap<>();
        fixedContext.put("environment", "dev");
        fixedContext.put("network", "internal");

        Configuration configuration = Configuration.load(loader, fixedContext);

        HashMap<String, String> context = new HashMap<>();

        Configuration.Projection projection = configuration.project(context);

        assertEquals("GET", projection.getText("routes.main_route.method"));

        System.setProperty("routes.main_route.method", "PUT");

        // continued unchanged
        assertEquals("GET", projection.getText("routes.main_route.method"));

        assertEquals("www.example-dev.com", projection.getText("service_x.api_config.endpoint"));
    }

    @Test
    public void testMustOverride() throws IOException {
        Loader loader = getLoader("example1");

        Map<String, String> fixedContext = new HashMap<>();
        fixedContext.put("environment", "dev");
        fixedContext.put("network", "internal");

        Configuration configuration = Configuration.load(loader, fixedContext);

        HashMap<String, String> context = new HashMap<>();

        Configuration.Projection projection = configuration.project(context, true);

        assertEquals("GET", projection.getText("routes.main_route.method"));

        System.setProperty("routes.main_route.method", "PUT");

        // must have unchanged
        assertEquals("PUT", projection.getText("routes.main_route.method"));

        assertEquals(58741503419348L, projection.getLong("crumb.limit"));

        System.setProperty("crumb.limit", "123871");

        // must have unchanged
        assertEquals(123871, projection.getLong("crumb.limit"));

        assertEquals("www.example-dev.com", projection.getText("service_x.api_config.endpoint"));
    }
}
