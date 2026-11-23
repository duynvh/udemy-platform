package com.marketplace.shared.domain;

import java.util.Currency;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * An amount in minor units (cents) and an ISO 4217 currency. Integer minor units avoid
 * floating-point rounding errors in prices, refunds, and revenue shares.
 * Example: {@code new Money(1999, "USD")} is 19.99 USD.
 */
public record Money(long amountMinor, String currency) {

    private static final Pattern CURRENCY_FORMAT = Pattern.compile("[A-Z]{3}");
    private static final Set<String> ISO_CURRENCIES = Currency.getAvailableCurrencies().stream()
            .map(Currency::getCurrencyCode)
            .collect(Collectors.toUnmodifiableSet());

    public Money {
        if (amountMinor < 0) {
            throw new IllegalArgumentException("Amount " + amountMinor + " is invalid; expected >= 0 minor units");
        }
        if (!isIsoCurrency(currency)) {
            throw new IllegalArgumentException(
                    "Currency '" + currency + "' is invalid; expected a 3-letter ISO 4217 code such as 'USD'");
        }
    }

    /** Adds two amounts of the same currency. Example: 19.99 USD + 29.99 USD = 49.98 USD. */
    public Money plus(Money other) {
        if (!currency.equals(other.currency)) {
            throw new IllegalArgumentException(
                    "Cannot add " + other.currency + " to " + currency + "; expected the same currency");
        }
        return new Money(Math.addExact(amountMinor, other.amountMinor), currency);
    }

    private static boolean isIsoCurrency(String code) {
        if (code == null || !CURRENCY_FORMAT.matcher(code).matches()) {
            return false;
        }
        return ISO_CURRENCIES.contains(code);
    }
}
