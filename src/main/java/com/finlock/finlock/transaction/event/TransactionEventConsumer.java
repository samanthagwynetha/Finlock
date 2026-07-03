package com.finlock.finlock.transaction.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class TransactionEventConsumer {
    @KafkaListener(topics = TransactionEventProducer.TOPIC, groupId = "finlock-group")
    public void consume(TransactionEvent event) {
        log.info("Received TransactionEvent: {} {} {} to {}",
                event.getStatus(),
                event.getAmount(),
                event.getCurrency(),
                event.getRecipientEmail());
    }
}
