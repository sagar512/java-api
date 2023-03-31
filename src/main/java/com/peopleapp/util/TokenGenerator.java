package com.peopleapp.util;

import java.util.UUID;

public class TokenGenerator {

    private TokenGenerator() {
    }



    /*
     *
     * Generate Token
     *
     */
    public static String generateTempToken() {
        return UUID.randomUUID().toString();
    }

    public static String generateSessionToken() {
        return UUID.randomUUID().toString();
    }
}
