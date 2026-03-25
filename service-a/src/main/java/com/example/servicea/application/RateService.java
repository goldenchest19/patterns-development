package com.example.servicea.application;

import com.example.servicea.domain.RateQuote;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class RateService {
    private static final Logger logger = LoggerFactory.getLogger(RateService.class);
    private final RateProvider rateProvider;

    public RateService(RateProvider rateProvider) {
        this.rateProvider = rateProvider;
    }

    public RateQuote getRate(String baseCurrency, String quoteCurrency) {
        logger.info("Getting rate for currency {} and quote {}", baseCurrency, quoteCurrency);
        return rateProvider.getRate(baseCurrency, quoteCurrency);
    }
}
