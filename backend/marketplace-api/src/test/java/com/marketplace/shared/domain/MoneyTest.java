package com.marketplace.shared.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class MoneyTest {

    @Test
    void plus_sameCurrency_addsAmounts() {
        Money total = new Money(1999, "USD").plus(new Money(2999, "USD"));

        assertThat(total).isEqualTo(new Money(4998, "USD"));
    }

    @Test
    void plus_differentCurrency_throwsIllegalArgument() {
        Money dollars = new Money(1999, "USD");
        Money euros = new Money(1999, "EUR");

        assertThatThrownBy(() -> dollars.plus(euros))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("USD")
                .hasMessageContaining("EUR");
    }

    @Test
    void new_negativeAmount_throwsIllegalArgument() {
        assertThatThrownBy(() -> new Money(-1, "USD"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("-1")
                .hasMessageContaining(">= 0");
    }

    @Test
    void new_lowercaseCurrency_throwsIllegalArgument() {
        assertThatThrownBy(() -> new Money(100, "usd"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("'usd'")
                .hasMessageContaining("ISO 4217");
    }

    @Test
    void new_unknownCurrency_throwsIllegalArgument() {
        assertThatThrownBy(() -> new Money(100, "QQQ"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("'QQQ'");
    }

    @Test
    void new_nullCurrency_throwsIllegalArgument() {
        assertThatThrownBy(() -> new Money(100, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("'null'");
    }
}
