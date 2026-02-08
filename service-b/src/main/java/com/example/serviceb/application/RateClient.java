package com.example.serviceb.application;

import com.example.serviceb.domain.RateQuote;

public interface RateClient {
    RateQuote getRate(String baseCurrency, String quoteCurrency);
}
