package com.example.serviceb.contract;

import au.com.dius.pact.consumer.MessagePactBuilder;
import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.consumer.junit5.ProviderType;
import au.com.dius.pact.core.model.PactSpecVersion;
import au.com.dius.pact.core.model.annotations.Pact;
import au.com.dius.pact.core.model.messaging.Message;
import au.com.dius.pact.core.model.messaging.MessagePact;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(PactConsumerTestExt.class)
class RateProviderMessagePactTest {

    @Pact(consumer = "rate-printer", provider = "rate-provider")
    MessagePact rateMessagePact(MessagePactBuilder builder) {
        PactDslJsonBody body = new PactDslJsonBody()
                .stringMatcher("pair", "[A-Z]{6}", "USDRUB")
                .decimalType("rate", 90.15)
                .numberType("timestampEpochMillis", 1700000000000L);

        return builder
                .expectsToReceive("a current USD/RUB rate message")
                .withMetadata(Map.of("contentType", "application/json"))
                .withContent(body)
                .toPact();
    }

    @Test
    @PactTestFor(pactMethod = "rateMessagePact", providerType = ProviderType.ASYNCH, pactVersion = PactSpecVersion.V3)
    void shouldConsumeRateMessage(List<Message> messages) {
        assertEquals(1, messages.size());

        String json = new String(messages.get(0).contentsAsBytes(), StandardCharsets.UTF_8);
        assertTrue(json.contains("\"pair\""));
        assertTrue(json.contains("\"rate\""));
        assertTrue(json.contains("\"timestampEpochMillis\""));
    }
}
