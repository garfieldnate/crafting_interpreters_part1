package com.craftinginterpreters.lox;

import java.io.Serial;

class RuntimeError extends RuntimeException {
    @Serial
    private static final long serialVersionUID = -874367121191485468L;
    final Token token;

    RuntimeError(final Token token, final String message) {
        super(message);
        this.token = token;
    }
}