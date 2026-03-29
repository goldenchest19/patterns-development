package com.example.servicea.infrastructure.rate;

import com.example.servicea.application.RateProvider;
import com.example.servicea.domain.RateQuote;
import com.example.servicea.infrastructure.config.RateProperties;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

@Component
public class RandomRateProvider implements RateProvider {
    private final RateProperties properties;

    public RandomRateProvider(RateProperties properties) {
        this.properties = properties;
    }

    @Override
    public RateQuote getRate(String baseCurrency, String quoteCurrency) {
        String base = normalizeCurrency(baseCurrency, "USD");
        String quote = normalizeCurrency(quoteCurrency, "RUB");
        String pair = base + quote;
        double rate = properties.getBaseValue() + ThreadLocalRandom.current()
                .nextDouble(-properties.getSpread(), properties.getSpread());
        return new RateQuote(pair, rate, Instant.now());
    }

    private String normalizeCurrency(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim().toUpperCase(Locale.US);
    }
}
