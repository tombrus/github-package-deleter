package com.tombrus.githubPackageDeleter;

import java.io.IOException;

public class ConnectionError extends RuntimeException {
    public ConnectionError(IOException e) {
        super(e.getMessage(), e);
    }

    public ConnectionError(String msg) {
        super(msg);
    }
}
