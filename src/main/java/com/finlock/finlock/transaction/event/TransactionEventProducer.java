package com.finlock.finlock.transaction.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionEventProducer {

    private final KafkaTemplate<String, TransactionEvent> kafkaTemplate;

    public static final String TOPIC = "finlock.transactions";


    public void publish(TransactionEvent event) {

        System.out.println("PUBLISH CALLED for: " + event.getTransactionId());

        log.info("Attempting to publish TransactionEvent for transaction {}", event.getTransactionId());

        kafkaTemplate.send(TOPIC, event.getTransactionId().toString(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("KAFKA PUBLISH FAILED for transaction {}: {}",
                                event.getTransactionId(), ex.getMessage());
                        System.err.println("KAFKA ERROR: " + ex.getMessage());
                        ex.printStackTrace();
                    } else {
                        log.info("KAFKA PUBLISH SUCCESS for transaction {} offset {}",
                                event.getTransactionId(),
                                result.getRecordMetadata().offset());
                    }
                });
    }
}
