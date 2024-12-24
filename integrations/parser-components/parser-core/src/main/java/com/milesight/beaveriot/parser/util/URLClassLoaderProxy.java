package com.milesight.beaveriot.parser.util;

import java.net.URL;
import java.net.URLClassLoader;

public class URLClassLoaderProxy extends URLClassLoader {
    public URLClassLoaderProxy(URL[] urls, ClassLoader parent) {
        super(urls, parent);
    }

    @Override
    protected void addURL(URL url) {
        super.addURL(url);
    }
}
