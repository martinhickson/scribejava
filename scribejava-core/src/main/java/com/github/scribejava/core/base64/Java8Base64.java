package com.github.scribejava.core.base64;

import java.lang.reflect.Method;

public class Java8Base64 extends Base64 {

    private static final Object JAVA8_BASE64;
    private static final Method ENCODE;
    private static final Method ENCODE_URL_WITHOUT_PADDING;

    static {
        // Reflective load. A direct reference fails class initialization when the
        // optional scribejava-java8 jar is absent (issue 1028).
        Object encoder = null;
        Method encode = null;
        Method encodeUrlWithoutPadding = null;
        try {
            Class.forName("java.util.Base64", false, Java8Base64.class.getClassLoader());
            final Class<?> type = Class.forName("com.github.scribejava.java8.base64.Java8Base64", false,
                    Java8Base64.class.getClassLoader());
            encoder = type.getConstructor().newInstance();
            encode = type.getMethod("internalEncode", byte[].class);
            encodeUrlWithoutPadding = type.getMethod("internalEncodeUrlWithoutPadding", byte[].class);
        } catch (final ClassNotFoundException ex) {
            encoder = null;
            encode = null;
            encodeUrlWithoutPadding = null;
        } catch (final ReflectiveOperationException ex) {
            encoder = null;
            encode = null;
            encodeUrlWithoutPadding = null;
        } catch (final LinkageError ex) {
            encoder = null;
            encode = null;
            encodeUrlWithoutPadding = null;
        }
        JAVA8_BASE64 = encoder;
        ENCODE = encode;
        ENCODE_URL_WITHOUT_PADDING = encodeUrlWithoutPadding;
    }

    @Override
    protected String internalEncode(byte[] bytes) {
        try {
            return (String) ENCODE.invoke(JAVA8_BASE64, new Object[]{bytes});
        } catch (final ReflectiveOperationException ex) {
            throw new IllegalStateException(ex);
        }
    }

    @Override
    protected String internalEncodeUrlWithoutPadding(byte[] bytes) {
        try {
            return (String) ENCODE_URL_WITHOUT_PADDING.invoke(JAVA8_BASE64, new Object[]{bytes});
        } catch (final ReflectiveOperationException ex) {
            throw new IllegalStateException(ex);
        }
    }

    static boolean isAvailable() {
        return JAVA8_BASE64 != null;
    }
}
