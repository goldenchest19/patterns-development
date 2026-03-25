package com.example.servicea.application;

import com.example.servicea.domain.RateQuote;

public interface RateProvider {
    RateQuote getRate(String baseCurrency, String quoteCurrency);
}
