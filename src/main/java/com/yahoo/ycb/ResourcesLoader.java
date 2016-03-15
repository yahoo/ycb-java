/*
 * Copyright 2016 Yahoo inc.
 * Licensed under the terms of the BSD License. Please see LICENSE file in the project home directory for terms.
 */

package com.yahoo.ycb;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.*;
import java.util.List;
import java.util.Set;

public class ResourcesLoader implements Loader {

    private final String dimensions;
    private final String[] configResources;

    public ResourcesLoader(String dimensionsResource, String... configResources) {
        this.dimensions = dimensionsResource;
        this.configResources = configResources;
    }

    @Override
    public List<Dimension> getDimensions() throws IOException {
        final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

        final JsonNode node = mapper.readTree(Thread.currentThread().getContextClassLoader().getResourceAsStream(dimensions));

        return LoadUtils.parseDimensions(node);
    }

    @Override
    public Set<Bundle> getBundles() throws IOException {
        final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

        final JsonNode node = mapper.readTree(getConfigInputStream());

        return LoadUtils.parseBundles(node);
    }

    private InputStream getConfigInputStream() {
        InputStream result = new ByteArrayInputStream(new byte[]{});
        final ClassLoader loader = Thread.currentThread().getContextClassLoader();

        for (String configResource : configResources) {
            result = new SequenceInputStream(
                    result,
                    new SequenceInputStream(
                            // make sure we have a line break between files
                            new ByteArrayInputStream("\n".getBytes()),
                            loader.getResourceAsStream((configResource))));
        }
        return result;
    }
}
