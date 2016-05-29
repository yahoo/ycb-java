/*
 * Copyright 2015 Yahoo inc.
 * Licensed under the terms of the BSD License. Please see LICENSE file in the project home directory for terms.
 */

package com.yahoo.ycb;

import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class ConfigurationTest {

    @Test
    public void testConfiguration() throws IOException {
        Loader loader = TestUtils.getLoader("example1");

        Configuration configuration = Configuration.load(loader);

        HashMap<String, String> context = new HashMap<>();

        Configuration.Projection projection = configuration.project(context);

        assertEquals(10, projection.getInteger("feature.functionality_a"));
        assertEquals(10, projection.getInteger("feature.functionality_b"));
        assertEquals(10, projection.getInteger("feature.functionality_c"));
        assertEquals(10, projection.getInteger("feature.functionality_d"));
        assertEquals(10, projection.getInteger("feature.functionality_e"));
        assertEquals(20, projection.getInteger("feature.functionality_f"));
        assertEquals("www.example-prod.com", projection.getText("service_x.api_config.endpoint"));

        context = new HashMap<>();
        context.put("network", "internal");

        projection = configuration.project(context);
        assertEquals(context, projection.getContext());

        assertEquals(20, projection.getInteger("feature.functionality_a"));
        assertEquals(10, projection.getInteger("feature.functionality_b"));
        assertEquals(10, projection.getInteger("feature.functionality_c"));
        assertEquals(10, projection.getInteger("feature.functionality_d"));
        assertEquals(10, projection.getInteger("feature.functionality_e"));
        assertEquals(20, projection.getInteger("feature.functionality_f"));
        assertEquals("www.example-prod.com", projection.getText("service_x.api_config.endpoint"));

        context.put("environment", "dev");
        projection = configuration.project(context);
        assertEquals(context, projection.getContext());

        assertEquals(20, projection.getInteger("feature.functionality_a"));
        assertEquals(20, projection.getInteger("feature.functionality_b"));
        assertEquals(20, projection.getInteger("feature.functionality_c"));
        assertEquals(10, projection.getInteger("feature.functionality_d"));
        assertEquals(20, projection.getInteger("feature.functionality_e"));
        assertEquals(20, projection.getInteger("feature.functionality_f"));
        assertEquals("www.example-dev.com", projection.getText("service_x.api_config.endpoint"));

        context = new HashMap<>();
        context.put("environment", "demo"); // demo is dev
        projection = configuration.project(context);
        assertEquals(context, projection.getContext());

        assertEquals(10, projection.getInteger("feature.functionality_a"));
        assertEquals(20, projection.getInteger("feature.functionality_b"));
        assertEquals(20, projection.getInteger("feature.functionality_c"));
        assertEquals(10, projection.getInteger("feature.functionality_d"));
        assertEquals(20, projection.getInteger("feature.functionality_e"));
        assertEquals(20, projection.getInteger("feature.functionality_f"));
        assertEquals("www.example-dev.com", projection.getText("service_x.api_config.endpoint"));
    }

    @Test
    public void testFixedContext() throws IOException {
        Loader loader = TestUtils.getLoader("example1");

        Map<String, String> fixedContext = new HashMap<>();
        fixedContext.put("environment", "dev");
        fixedContext.put("network", "internal");

        Configuration configuration = Configuration.load(loader, fixedContext);

        HashMap<String, String> context = new HashMap<>();

        Configuration.Projection projection = configuration.project(context);

        assertEquals("www.service_y.com", projection.getText("service_y.hostname"));
        assertEquals("no", projection.getText("service_y.modules.generic"));
        assertEquals("GET", projection.getText("routes.main_route.method"));
        assertEquals("www.example-dev.com", projection.getText("service_x.api_config.endpoint"));

        context.put("bucket", "BUCKET_001");
        projection = configuration.project(context);

        assertEquals("yes", projection.getText("service_y.modules.generic"));
        assertEquals("www.example-dev.com", projection.getText("service_x.api_config.endpoint"));

        context = new HashMap<>();
        context.put("bucket", "BUCKET_006");
        projection = configuration.project(context);

        assertEquals("www.example-bucket_006-dev.com", projection.getText("service_x.api_config.endpoint"));
    }


    @Test
    public void testTotalFixedContext() throws IOException {
        Loader loader = TestUtils.getLoader("example1");

        Map<String, String> fixedContext = new HashMap<>();
        fixedContext.put("environment", "dev");
        fixedContext.put("bucket", "BUCKET_002");
        fixedContext.put("network", "internal");
        fixedContext.put("user_type", "premium");
        fixedContext.put("locale", "en-US");

        Configuration configuration = Configuration.load(loader, fixedContext);

        HashMap<String, String> context = new HashMap<>();

        Configuration.Projection projection = configuration.project(context);

        assertEquals("www.service_y.com", projection.getText("service_y.hostname"));
        assertEquals("no", projection.getText("service_y.modules.generic"));
        assertEquals("GET", projection.getText("routes.main_route.method"));
        assertEquals("www.example-dev.com", projection.getText("service_x.api_config.endpoint"));
    }

    @Test
    public void testDifferentPathSeparator() throws IOException {
        Loader loader = TestUtils.getLoader("example1");

        Configuration configuration = Configuration.load(loader);
        configuration.setPathSeparator("\\/");

        assertEquals("\\/",  configuration.getPathSeparator());

        HashMap<String, String> context = new HashMap<>();

        Configuration.Projection projection = configuration.project(context);

        assertEquals(10, projection.getInteger("feature/functionality_a"));
        assertEquals(10, projection.getInteger("feature/functionality_b"));
        assertEquals(10, projection.getInteger("feature/functionality_c"));
        assertEquals(10, projection.getInteger("feature/functionality_d"));
        assertEquals(10, projection.getInteger("feature/functionality_e"));
        assertEquals(20, projection.getInteger("feature/functionality_f"));
        assertEquals("no", projection.getText("service_y/modules/generic"));
    }

    @Test
    public void testGetList() throws IOException {
        Loader loader = TestUtils.getLoader("example1");

        Configuration configuration = Configuration.load(loader);

        HashMap<String, String> context = new HashMap<>();

        Configuration.Projection projection = configuration.project(context);

        List<String> result = projection.getList("service_x.api_config.params.user_query");

        assertEquals(2, result.size());
        assertEquals("spam", result.get(0));
        assertEquals("eggs", result.get(1));

        result = projection.getList("feature/functionality_c");

        // on error, it should just return NULL
        assertNull(result);
    }

    @Test
    public void testGetObject() throws IOException {
        Loader loader = TestUtils.getLoader("example1");

        Configuration configuration = Configuration.load(loader);

        HashMap<String, String> context = new HashMap<>();
        context.put("user_type", "premium");

        Configuration.Projection projection = configuration.project(context);

        Map<String, Integer> result = projection.getObject("crumb.params", Map.class);

        assertEquals(3, result.size());
        assertEquals(10, (int) result.get("x"));
        assertEquals(20, (int) result.get("y"));
        assertEquals(40, (int) result.get("z"));
    }

    @Test
    public void testGetSimpleTypes() throws IOException {
        Loader loader = TestUtils.getLoader("example1");

        Configuration configuration = Configuration.load(loader);

        HashMap<String, String> context = new HashMap<>();
        context.put("user_type", "premium");

        Configuration.Projection projection = configuration.project(context);

        assertTrue(projection.getBoolean("crumb.enabled"));
        assertEquals(0.83123, projection.getDouble("crumb.alpha"), 0.000001);
        assertEquals(58741503419348L, projection.getLong("crumb.limit"));
    }

    @Test
    public void testTraverseContext() throws IOException {
        Loader loader = TestUtils.getLoader("example2");

        Configuration configuration = Configuration.load(loader);

        List<Map<String, String>> contexts = configuration.traverseContexts(loader.getDimensions());

        assertEquals(3 * 3 * 3 * 3, contexts.size());

        final String[][] expected = {
                {"*", "*", "*", "*"},
                {"*", "*", "*", "BUCKET_A"},
                {"*", "*", "*", "BUCKET_AB"},
                {"*", "*", "external", "*"},
                {"*", "*", "external", "BUCKET_A"},
                {"*", "*", "external", "BUCKET_AB"},
                {"*", "*", "internal", "*"},
                {"*", "*", "internal", "BUCKET_A"},
                {"*", "*", "internal", "BUCKET_AB"},
                {"*", "east", "*", "*"},
                {"*", "east", "*", "BUCKET_A"},
                {"*", "east", "*", "BUCKET_AB"},
                {"*", "east", "external", "*"},
                {"*", "east", "external", "BUCKET_A"},
                {"*", "east", "external", "BUCKET_AB"},
                {"*", "east", "internal", "*"},
                {"*", "east", "internal", "BUCKET_A"},
                {"*", "east", "internal", "BUCKET_AB"},
                {"*", "west", "*", "*"},
                {"*", "west", "*", "BUCKET_A"},
                {"*", "west", "*", "BUCKET_AB"},
                {"*", "west", "external", "*"},
                {"*", "west", "external", "BUCKET_A"},
                {"*", "west", "external", "BUCKET_AB"},
                {"*", "west", "internal", "*"},
                {"*", "west", "internal", "BUCKET_A"},
                {"*", "west", "internal", "BUCKET_AB"},
                {"production", "*", "*", "*"},
                {"production", "*", "*", "BUCKET_A"},
                {"production", "*", "*", "BUCKET_AB"},
                {"production", "*", "external", "*"},
                {"production", "*", "external", "BUCKET_A"},
                {"production", "*", "external", "BUCKET_AB"},
                {"production", "*", "internal", "*"},
                {"production", "*", "internal", "BUCKET_A"},
                {"production", "*", "internal", "BUCKET_AB"},
                {"production", "east", "*", "*"},
                {"production", "east", "*", "BUCKET_A"},
                {"production", "east", "*", "BUCKET_AB"},
                {"production", "east", "external", "*"},
                {"production", "east", "external", "BUCKET_A"},
                {"production", "east", "external", "BUCKET_AB"},
                {"production", "east", "internal", "*"},
                {"production", "east", "internal", "BUCKET_A"},
                {"production", "east", "internal", "BUCKET_AB"},
                {"production", "west", "*", "*"},
                {"production", "west", "*", "BUCKET_A"},
                {"production", "west", "*", "BUCKET_AB"},
                {"production", "west", "external", "*"},
                {"production", "west", "external", "BUCKET_A"},
                {"production", "west", "external", "BUCKET_AB"},
                {"production", "west", "internal", "*"},
                {"production", "west", "internal", "BUCKET_A"},
                {"production", "west", "internal", "BUCKET_AB"},
                {"stage", "*", "*", "*"},
                {"stage", "*", "*", "BUCKET_A"},
                {"stage", "*", "*", "BUCKET_AB"},
                {"stage", "*", "external", "*",},
                {"stage", "*", "external", "BUCKET_A"},
                {"stage", "*", "external", "BUCKET_AB"},
                {"stage", "*", "internal", "*"},
                {"stage", "*", "internal", "BUCKET_A"},
                {"stage", "*", "internal", "BUCKET_AB"},
                {"stage", "east", "*", "*"},
                {"stage", "east", "*", "BUCKET_A"},
                {"stage", "east", "*", "BUCKET_AB"},
                {"stage", "east", "external", "*"},
                {"stage", "east", "external", "BUCKET_A"},
                {"stage", "east", "external", "BUCKET_AB"},
                {"stage", "east", "internal", "*"},
                {"stage", "east", "internal", "BUCKET_A"},
                {"stage", "east", "internal", "BUCKET_AB"},
                {"stage", "west", "*", "*"},
                {"stage", "west", "*", "BUCKET_A"},
                {"stage", "west", "*", "BUCKET_AB"},
                {"stage", "west", "external", "*"},
                {"stage", "west", "external", "BUCKET_A"},
                {"stage", "west", "external", "BUCKET_AB"},
                {"stage", "west", "internal", "*"},
                {"stage", "west", "internal", "BUCKET_A"},
                {"stage", "west", "internal", "BUCKET_AB"}
        };

        int i = 0;
        for (Map<String, String> ctx : contexts) {
            assertEquals("Unexpected value in index " + i, expected[i][0], ctx.get("environment"));
            assertEquals("Unexpected value in index " + i, expected[i][1], ctx.get("cluster"));
            assertEquals("Unexpected value in index " + i, expected[i][2], ctx.get("network"));
            assertEquals("Unexpected value in index " + i, expected[i][3], ctx.get("bucket"));

            i++;
        }
    }
}
