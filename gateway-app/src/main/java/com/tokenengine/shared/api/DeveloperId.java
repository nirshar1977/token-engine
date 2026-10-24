package com.tokenengine.shared.api;

import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Objects;

/** The developer a request is attributed to. Never holds a credential, only a display name. */
public record DeveloperId(@JsonValue String value) {

    public static final DeveloperId ANONYMOUS = new DeveloperId("anonymous");

    public DeveloperId {
        Objects.requireNonNull(value, "value");
        if (value.isBlank()) {
            throw new IllegalArgumentException("developer id must not be blank");
        }
    }

    @Override
    public String toString() {
        return value;
    }
}
