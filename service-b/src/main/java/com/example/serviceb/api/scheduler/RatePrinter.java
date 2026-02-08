package com.example.serviceb.api.scheduler;

import com.example.serviceb.application.RateClient;
import com.example.serviceb.domain.RateQuote;
import com.example.serviceb.infrastructure.config.RateProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class RatePrinter {
    private static final Logger LOGGER = LoggerFactory.getLogger(RatePrinter.class);

    private final RateClient rateClient;
    private final RateProperties properties;

    public RatePrinter(RateClient rateClient, RateProperties properties) {
        this.rateClient = rateClient;
        this.properties = properties;
    }

    @Scheduled(fixedDelayString = "${rate.poll-interval:5000}")
    public void printRate() {
        try {
            RateQuote quote = rateClient.getRate(properties.getBaseCurrency(), properties.getQuoteCurrency());
            LOGGER.info("Rate {} = {} (ts={})", quote.pair(), quote.rate(), quote.timestamp());
        } catch (Exception ex) {
            LOGGER.warn("Failed to fetch rate", ex);
        }
    }
}
