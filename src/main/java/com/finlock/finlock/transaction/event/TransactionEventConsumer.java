package com.finlock.finlock.transaction.event;

import com.finlock.finlock.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(name = "spring.kafka.bootstrap-servers")
public class TransactionEventConsumer {

    private final NotificationService notificationService;

    @KafkaListener(
            topics = TransactionEventProducer.TOPIC,
            groupId = "finlock-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(TransactionEvent event) {
        log.info("Processing TransactionEvent for transaction {}", event.getTransactionId());

        if ("COMPLETED".equals(event.getStatus())) {
            notificationService.notifyTransfer(
                    event.getSenderEmail(),
                    event.getRecipientEmail(),
                    event.getAmount(),
                    event.getCurrency()
            );
        }

        log.info("Successfully processed TransactionEvent for transaction {}",
                event.getTransactionId());
    }
}