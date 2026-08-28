package com.finlock.finlock.transaction.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@ConditionalOnProperty(name = "spring.kafka.bootstrap-servers")
public class DeadLetterConsumer {

    @KafkaListener(
            topics = TransactionEventProducer.TOPIC + "-dlt",
            groupId = "finlock-dlq-group"
    )
    public void consumeDeadLetter(
            String message,
            @Header(value = "kafka_dlt-exception-message", required = false)
            String exceptionMessage) {

        log.error("🚨 [DEAD LETTER] Failed message received.");
        log.error("🚨 [DEAD LETTER] Reason: {}", exceptionMessage);
        log.error("🚨 [DEAD LETTER] Raw message: {}", message);
    }
}