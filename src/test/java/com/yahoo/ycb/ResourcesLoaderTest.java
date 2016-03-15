/*
 * Copyright 2016 Yahoo inc.
 * Licensed under the terms of the BSD License. Please see LICENSE file in the project home directory for terms.
 */

package com.yahoo.ycb;

import org.junit.Test;

import java.io.IOException;
import java.util.HashMap;

import static org.junit.Assert.*;

public class ResourcesLoaderTest {

    @Test
    public void testConfiguration() throws IOException {
        Loader loader = new ResourcesLoader("example1/dimensions.yml",
                "example1/crumb.yml", "example1/features.yml", "example1/routes.yml", "example1/service_x.yml", "example1/service_y.yml");

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
}