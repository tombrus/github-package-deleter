package com.tombrus.githubPackageDeleter;

public final class TokenStore {
    private static String token;

    private TokenStore() {
    }

    public static void setToken(String token) {
        TokenStore.token = token;
    }

    public static String getToken() {
        return token;
    }
}
