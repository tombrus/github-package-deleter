package com.tombrus.githubPackageDeleter;

import org.modelingvalue.json.Json;
import org.modelingvalue.json.JsonPrettyfier;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import javax.swing.*;

import static java.nio.charset.StandardCharsets.UTF_8;

public class GraphQL<A> {
    private static final boolean   TRACE          = Boolean.getBoolean("TRACE");
    private final static String    GITHUB_API_URL = "https://api.github.com/graphql";
    private final static String    QUERY_TEMPLATE = "{\"query\":\"%s\"}";
    protected final      Class<A>  answerClass;
    private final        boolean   packageDeletesPreview;
    private              RateLimit rateLimit;

    public GraphQL(Class<A> answerClass, boolean packageDeletesPreview) {
        this.answerClass           = answerClass;
        this.packageDeletesPreview = packageDeletesPreview;
    }

    public RateLimit getRateLimit() {
        return rateLimit;
    }

    protected A query(String graphql) {
        return run(graphql);
    }

    protected CompletableFuture<A> queryAsync(String graphql) {
        return CompletableFuture.supplyAsync(() -> query(graphql));
    }

    private A run(String graphql) {
        try {
            String            sanatizedQuery = graphql.replace("\"", "\\\"").replaceAll("\n", "\\\\n");
            HttpURLConnection conn           = (HttpURLConnection) new URL(GITHUB_API_URL).openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Authorization", "bearer " + TokenStore.getToken());
            conn.setRequestProperty("Accept-Encoding", "json");
            conn.setRequestProperty("Content-Type", "application/json");
            if (packageDeletesPreview) {
                conn.setRequestProperty("Accept", "application/vnd.github.package-deletes-preview+json");
            }
            conn.setDoOutput(true);
            try (OutputStream os = conn.getOutputStream()) {
                os.write(String.format(QUERY_TEMPLATE, sanatizedQuery).getBytes(StandardCharsets.UTF_8));
            }
            conn.connect();
            int responseCode = conn.getResponseCode();
            if (responseCode == 500) {
                throw new InternalServerError();
            }
            if (responseCode != 200) {
                throw new IOException("can not connect: " + responseCode);
            }
            String json;
            try (InputStream is = conn.getInputStream()) {
                json = new String(is.readAllBytes(), UTF_8);
            }
            rateLimit = new RateLimit(conn);

            if (TRACE) {
                System.err.println("========================================================================================================================================");
                System.err.println("JSON-" + getClass().getSimpleName() + ": ");
                System.err.println(U.addLineNumbers(graphql));
                System.err.println("========================================================================================================================================");
                System.err.println(JsonPrettyfier.pretty(json));
                System.err.println("========================================================================================================================================");
            }

            return Json.fromJson(answerClass, json);
        } catch (IOException e) {
            throw new ConnectionError(e);
        }
    }

    public record RateLimit(long limit, long remaining, long reset, long used, String resource) {
        public RateLimit(HttpURLConnection conn) {
            this(Long.parseLong(conn.getHeaderField("X-RateLimit-Limit")),
                    Long.parseLong(conn.getHeaderField("X-RateLimit-Remaining")),
                    Long.parseLong(conn.getHeaderField("X-RateLimit-Reset")),
                    Long.parseLong(conn.getHeaderField("X-RateLimit-Used")),
                    conn.getHeaderField("X-RateLimit-Resource"));
            SwingUtilities.invokeLater(() -> GithubPackageDeleter.INSTANCE.rateLimitLabel.setText("" + remaining));
        }
    }

    public static class PageInfo {
        boolean hasNextPage;
        String  endCursor;
    }

    @SuppressWarnings("unused")
    public static class ErrorInfo {
        String         message;
        String         type;
        List<String>   path;
        List<Location> locations;
        Extensions     extensions;
    }

    @SuppressWarnings("unused")
    public static class Location {
        int line;
        int column;
    }

    @SuppressWarnings("unused")
    public static class Extensions {
        String code;
        String name;
        String typeName;
        String argumentName;
        String fieldName;
    }
}
