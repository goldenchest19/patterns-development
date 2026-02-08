package com.example.serviceb.domain;

import java.time.Instant;

public record RateQuote(String pair, double rate, Instant timestamp) {
}
