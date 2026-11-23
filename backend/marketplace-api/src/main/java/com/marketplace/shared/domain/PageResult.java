package com.marketplace.shared.domain;

import java.util.List;
import java.util.function.Function;

/**
 * One page of items plus the total count of all matching items.
 * Example: {@code courses.map(CourseView::from)} converts items and keeps {@code totalItems}.
 */
public record PageResult<T>(List<T> items, long totalItems) {

    public PageResult {
        if (totalItems < 0) {
            throw new IllegalArgumentException("Total items " + totalItems + " is invalid; expected >= 0");
        }
        // Defensive copy: a caller cannot change the page after it is created.
        items = List.copyOf(items);
    }

    /** Converts each item; the total count stays the same. */
    public <R> PageResult<R> map(Function<? super T, ? extends R> converter) {
        List<R> converted = items.stream().<R>map(converter).toList();
        return new PageResult<>(converted, totalItems);
    }
}
