package com.finlock.finlock.notification.service;

import com.finlock.finlock.auth.repository.UserRepository;
import com.finlock.finlock.notification.entity.Notification;
import com.finlock.finlock.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public void notifyTransfer(String senderEmail, String recipientEmail,
                               BigDecimal amount, String currency) {

        sendEmail(
                senderEmail,
                "Transfer Sent — FinLock",
                String.format("You successfully sent %s %s to %s. Your transfer is complete.",
                        amount, currency, recipientEmail)
        );

        sendEmail(
                recipientEmail,
                "You Received Money — FinLock",
                String.format("You received %s %s from %s. Check your wallet balance.",
                        amount, currency, senderEmail)
        );
    }

    private void sendEmail(String toEmail, String subject, String message) {

        log.info("📧 [EMAIL SIMULATED] To: {} | Subject: {} | Message: {}",
                toEmail, subject, message);

        UUID userId = userRepository.findByEmail(toEmail)
                .map(user -> user.getId())
                .orElse(null);

        try {
            Notification notification = Notification.builder()
                    .userId(userId)
                    .type("EMAIL")
                    .recipient(toEmail)
                    .subject(subject)
                    .message(message)
                    .status("SENT")
                    .build();

            notificationRepository.save(notification);
        } catch (Exception e) {
            log.error("Failed to save notification record for {}: {}", toEmail, e.getMessage());
        }
    }
}