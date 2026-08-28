package com.finlock.finlock.transaction.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(name = "spring.kafka.bootstrap-servers")
public class TransactionEventProducer {

    private final KafkaTemplate<String, TransactionEvent> kafkaTemplate;

    public static final String TOPIC = "finlock.transactions";

    public void publish(TransactionEvent event) {
        kafkaTemplate.send(TOPIC, event.getTransactionId().toString(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish TransactionEvent for transaction {}: {}",
                                event.getTransactionId(), ex.getMessage());
                    } else {
                        log.info("Published TransactionEvent for transaction {} at offset {}",
                                event.getTransactionId(),
                                result.getRecordMetadata().offset());
                    }
                });
    }
}