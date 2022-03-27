package com.tombrus.githubPackageDeleter;

import java.util.concurrent.CompletableFuture;

public abstract class Paginator<A, R> extends GraphQL<A> {
    private static final int PAGE_SIZE = 100;
    private static final int NUM_TRIES = 10;

    public Paginator(Class<A> answerClass) {
        super(answerClass, false);
    }

    protected abstract String getQueryWithPagination();

    protected abstract PageInfo pageInfo(A answer);

    protected abstract R harvest(R preResult, A answer);

    public CompletableFuture<R> start() {
        return CompletableFuture.supplyAsync(this::paginate);
    }

    private R paginate() {
        for (int tries = 0; tries < NUM_TRIES; tries++) {
            try {
                A answer = null;
                R result = null;
                do {
                    String selector = String.format("first:%d", PAGE_SIZE);
                    if (answer != null) {
                        selector += String.format("after:\"%s\"", pageInfo(answer).endCursor);
                    }
                    answer = query(getQueryWithPagination().replace("@SELECTOR@", selector));
                    result = harvest(result, answer);
                } while (pageInfo(answer).hasNextPage);
                return result;
            } catch (InternalServerError e) {
                e.printStackTrace();
                System.err.println("DARN.... (" + tries + ")" + e.getMessage());
            }
        }
        throw new ConnectionError("too many InternalServerErrors detected");
    }
}
