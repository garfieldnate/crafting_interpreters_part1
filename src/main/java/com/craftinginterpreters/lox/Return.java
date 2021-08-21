package com.craftinginterpreters.lox;

import java.io.Serial;

public class Return extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 7870140464090251903L;
    final Object value;

    public Return(final Object value) {
        super(null, null, false, false);
        this.value = value;
    }
}
