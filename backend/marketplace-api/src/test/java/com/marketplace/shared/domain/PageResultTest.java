package com.marketplace.shared.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class PageResultTest {

    @Test
    void map_twoItems_keepsTotal() {
        PageResult<String> titles = new PageResult<>(List.of("java", "react"), 42);

        PageResult<Integer> lengths = titles.map(String::length);

        assertThat(lengths.items()).containsExactly(4, 5);
        assertThat(lengths.totalItems()).isEqualTo(42);
    }

    @Test
    void new_mutableList_copiesItems() {
        List<String> source = new ArrayList<>(List.of("java"));
        PageResult<String> page = new PageResult<>(source, 1);

        source.add("react");

        assertThat(page.items()).containsExactly("java");
    }

    @Test
    void new_negativeTotal_throwsIllegalArgument() {
        assertThatThrownBy(() -> new PageResult<>(List.of(), -1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("-1")
                .hasMessageContaining(">= 0");
    }
}
