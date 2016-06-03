/*
 * Copyright 2016 Yahoo inc.
 * Licensed under the terms of the BSD License. Please see LICENSE file in the project home directory for terms.
 */

package com.yahoo.ycb;

import java.io.File;
import java.net.URL;

public class TestUtils {
    private TestUtils() {
    }

    public static Loader getLoader(String name) {
        final URL url = Thread.currentThread().getContextClassLoader().getResource(name);
        assert url != null;

        return new FileSystemLoader(new File(url.getPath()));
    }
}
