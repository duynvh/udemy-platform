package com.marketplace.shared.domain;

/**
 * A page request for list use cases. Framework-free, so ports do not depend on Spring Data.
 * Example: {@code new PageQuery(0, 20)} asks for the first 20 items.
 */
public record PageQuery(int page, int size) {

    public static final int MAX_SIZE = 100;

    public PageQuery {
        if (page < 0) {
            throw new IllegalArgumentException("Page " + page + " is invalid; expected >= 0");
        }
        if (size < 1 || size > MAX_SIZE) {
            throw new IllegalArgumentException("Page size " + size + " is invalid; expected 1-" + MAX_SIZE);
        }
    }
}
