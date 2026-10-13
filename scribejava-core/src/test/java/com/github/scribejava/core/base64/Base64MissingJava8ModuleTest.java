package com.github.scribejava.core.base64;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class Base64MissingJava8ModuleTest {

    @Test
    public void encodeWorksWhenJava8ModuleIsMissing() throws Exception {
        final ClassLoader hidden = new ClassLoader(Base64.class.getClassLoader()) {
            @Override
            protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
                if (name.startsWith("com.github.scribejava.java8.")
                        || name.startsWith("com.github.scribejava.core.base64.")) {
                    throw new ClassNotFoundException(name);
                }
                return super.loadClass(name, resolve);
            }
        };
        final URL classes = new File("target/classes").toURI().toURL();
        final URLClassLoader isolated = new URLClassLoader(new URL[]{classes}, hidden);
        try {
            final Class<?> base64 = isolated.loadClass("com.github.scribejava.core.base64.Base64");
            final String encoded = (String) base64.getMethod("encode", byte[].class)
                    .invoke(null, "Hello World".getBytes("UTF-8"));
            assertEquals("SGVsbG8gV29ybGQ=", encoded);
            final Object instance = base64.getMethod("getInstance").invoke(null);
            assertFalse(instance.getClass().getName().endsWith("Java8Base64"));
        } finally {
            isolated.close();
        }
    }
}
