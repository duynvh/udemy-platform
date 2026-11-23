package com.marketplace.shared.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class PageQueryTest {

    @Test
    void new_validValues_keepsValues() {
        PageQuery query = new PageQuery(2, 50);

        assertThat(query.page()).isEqualTo(2);
        assertThat(query.size()).isEqualTo(50);
    }

    @Test
    void new_negativePage_throwsIllegalArgument() {
        assertThatThrownBy(() -> new PageQuery(-1, 20))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("-1")
                .hasMessageContaining(">= 0");
    }

    @Test
    void new_sizeZero_throwsIllegalArgument() {
        assertThatThrownBy(() -> new PageQuery(0, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("0")
                .hasMessageContaining("1-100");
    }

    @Test
    void new_sizeAbove100_throwsIllegalArgument() {
        assertThatThrownBy(() -> new PageQuery(0, 101))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("101")
                .hasMessageContaining("1-100");
    }
}
