package com.randevupazaryeri.mail;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Sends plain-text transactional email through Resend's HTTPS API, or over SMTP when no Resend key is set.
 * Runs off the request thread so response time does not reveal whether an address exists. Recipients,
 * bodies and keys are never logged.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private static final URI RESEND_EMAILS = URI.create("https://api.resend.com/emails");

    private final ObjectProvider<JavaMailSender> mailSender;
    private final EmailProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    @Value("${spring.mail.host:}")
    private String host;

    public boolean isConfigured() {
        if (properties.getFrom() == null || properties.getFrom().isBlank()) {
            return false;
        }
        return properties.usesResend()
                || (host != null && !host.isBlank() && mailSender.getIfAvailable() != null);
    }

    @Async
    public void send(String to, String subject, String text) {
        if (!isConfigured()) {
            log.warn("Email skipped: set MAIL_FROM and either RESEND_API_KEY or MAIL_HOST");
            return;
        }
        if (properties.usesResend()) {
            sendWithResend(to, subject, text);
        } else {
            sendWithSmtp(to, subject, text);
        }
    }

    private void sendWithResend(String to, String subject, String text) {
        try {
            String body = objectMapper.writeValueAsString(Map.of(
                    "from", properties.getFrom().trim(),
                    "to", List.of(to),
                    "subject", subject,
                    "text", text));
            HttpRequest request = HttpRequest.newBuilder(RESEND_EMAILS)
                    .timeout(Duration.ofSeconds(15))
                    .header("Authorization", "Bearer " + properties.getResendApiKey().trim())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() / 100 != 2) {
                log.warn("Email delivery failed: Resend returned {} ({})", response.statusCode(), errorName(response.body()));
            }
        } catch (IOException ex) {
            log.warn("Email delivery failed: {}", ex.getClass().getSimpleName());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    private void sendWithSmtp(String to, String subject, String text) {
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

    /** Resend's error code, e.g. "validation_error"; the message may echo the recipient, so it is dropped. */
    private String errorName(String body) {
        try {
            return objectMapper.readTree(body).path("name").asText("unknown");
        } catch (JsonProcessingException ex) {
            return "unknown";
        }
    }
}
