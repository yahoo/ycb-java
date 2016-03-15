/*
 * Copyright 2015 Yahoo inc.
 * Licensed under the terms of the BSD License. Please see LICENSE file in the project home directory for terms.
 */

package com.yahoo.ycb;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.*;
import java.util.*;

/**
 * Implementation of a Loader from the file system.
 * <p>
 * Configuration files should live in a folder, and are encoded as JSON or YAML files.
 */
public class FileSystemLoader implements Loader {

    private final File directoryPath;

    /**
     * @param directoryPath Where to look for configuration files.
     */
    public FileSystemLoader(File directoryPath) {
        this.directoryPath = directoryPath;
    }

    private InputStream getConfigInputStream(FileFilter filter) {
        File[] files = directoryPath.listFiles(filter);

        try {
            InputStream result = new ByteArrayInputStream(new byte[]{});

            for (File file : files) {
                result = new SequenceInputStream(
                                    result,
                                    new SequenceInputStream(
                                            // make sure we have a line break between files
                                            new ByteArrayInputStream("\n".getBytes()),
                                            new FileInputStream(file)));
            }
            return result;
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public List<Dimension> getDimensions() throws IOException {
        final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

        final JsonNode node = mapper.readTree(getConfigInputStream(pathname -> pathname.isFile() &&
                "dimensions.json".equalsIgnoreCase(pathname.getName()) ||
                "dimensions.yaml".equalsIgnoreCase(pathname.getName()) ||
                "dimensions.yml".equalsIgnoreCase(pathname.getName())));

        return LoadUtils.parseDimensions(node);
    }

    public Set<Bundle> getBundles() throws IOException {
        final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

        final JsonNode node = mapper.readTree(getConfigInputStream(pathname -> pathname.isFile() &&
                !pathname.getName().startsWith("dimensions.") &&
                (pathname.getName().endsWith(".json") ||
                        pathname.getName().endsWith(".yml") ||
                        pathname.getName().endsWith(".yaml"))));

        return LoadUtils.parseBundles(node);
    }

}
