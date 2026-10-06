package com.randevupazaryeri.mail;

import com.randevupazaryeri.config.EmailProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Sends plain-text transactional email over SMTP. Runs off the request thread so response time does not
 * reveal whether an address exists. Recipients and bodies are never logged.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final ObjectProvider<JavaMailSender> mailSender;
    private final EmailProperties properties;

    @Value("${spring.mail.host:}")
    private String host;

    public boolean isConfigured() {
        return host != null && !host.isBlank()
                && properties.getFrom() != null && !properties.getFrom().isBlank()
                && mailSender.getIfAvailable() != null;
    }

    @Async
    public void send(String to, String subject, String text) {
        if (!isConfigured()) {
            log.warn("Email skipped: MAIL_HOST or MAIL_FROM is not configured");
            return;
        }
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(properties.getFrom().trim());
        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);
        try {
            mailSender.getObject().send(message);
        } catch (MailException ex) {
            log.warn("Email delivery failed: {}", ex.getClass().getSimpleName());
        }
    }
}
