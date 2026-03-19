package com.java.domain.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import com.java.config.NotificationMailProperties;
import com.java.domain.service.dto.NotificationMessage;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationMailSender {

    private final JavaMailSender mailSender;
    private final NotificationMailProperties properties;

    public void send(NotificationMessage message) {
        log.info("=== NotificationMailSender.send START ===");
        log.info("Mail enabled: {}", properties.enabled());
        log.info("Mail from: {}", properties.from());
        log.info("Mail recipient: {}", message != null ? message.recipient() : null);
        log.info("Mail subject: {}", message != null ? message.subject() : null);
        log.debug("Mail body: {}", message != null ? message.body() : null);

        if (!properties.enabled()) {
            log.warn("Mail sending is disabled, skipping send");
            return;
        }

        if (message == null) {
            log.error("NotificationMessage is null");
            throw new IllegalArgumentException("NotificationMessage must not be null");
        }

        if (isBlank(properties.from())) {
            log.error("Mail from is blank");
            throw new IllegalStateException("Mail from must not be blank");
        }

        if (isBlank(message.recipient())) {
            log.error("Mail recipient is blank");
            throw new IllegalArgumentException("Mail recipient must not be blank");
        }

        if (isBlank(message.subject())) {
            log.warn("Mail subject is blank");
        }

        if (isBlank(message.body())) {
            log.warn("Mail body is blank");
        }

        try {
            SimpleMailMessage mail = new SimpleMailMessage();
            mail.setFrom(properties.from());
            mail.setTo(message.recipient());
            mail.setSubject(message.subject());
            mail.setText(message.body());

            log.info("Sending email via JavaMailSender...");
            mailSender.send(mail);
            log.info("Email sent successfully to {}", message.recipient());
            log.info("=== NotificationMailSender.send END ===");
        } catch (MailException ex) {
            log.error("MailException while sending email to {}", message.recipient(), ex);
            throw ex;
        } catch (Exception ex) {
            log.error("Unexpected exception while sending email to {}", message.recipient(), ex);
            throw new IllegalStateException("Failed to send notification email", ex);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}