package com.finlock.finlock.transaction.event;

import com.finlock.finlock.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionEventConsumer {

    private final NotificationService notificationService;

    @KafkaListener(
            topics = TransactionEventProducer.TOPIC,
            groupId = "finlock-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(TransactionEvent event) {
        System.out.println("CONSUMER CALLED for: " + event.getTransactionId());
        log.info("Processing TransactionEvent for transaction {}", event.getTransactionId());

        if ("COMPLETED".equals(event.getStatus())) {
            try {
                notificationService.notifyTransfer(
                        event.getSenderEmail(),
                        event.getRecipientEmail(),
                        event.getAmount(),
                        event.getCurrency()
                );
            } catch (Exception e) {
                System.out.println("EXCEPTION CAUGHT IN CONSUMER: " + e.getMessage());
                // Re-throw so Kafka error handler can retry and route to DLT
                throw e;
            }
        }

        log.info("Successfully processed TransactionEvent for transaction {}",
                event.getTransactionId());
    }
}