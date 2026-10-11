package com.cognevance.ecommerce.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service @Slf4j
public class EmailService {
    private final ObjectProvider<JavaMailSender> sender;
    private final String from;

    public EmailService(ObjectProvider<JavaMailSender> sender, @Value("${app.mail.from}") String from) {
        this.sender = sender;
        this.from = from;
    }

    @Async
    public void send(String to, String subject, String body) {
        JavaMailSender s = sender.getIfAvailable();
        if (s == null) { log.info("[EMAIL disabled] To: {} | {} | {}", to, subject, body); return; }
        try {
            SimpleMailMessage m = new SimpleMailMessage();
            m.setFrom(from); m.setTo(to); m.setSubject(subject); m.setText(body);
            s.send(m);
        } catch (Exception e) {
            log.warn("Email to {} failed: {}", to, e.getMessage());
        }
    }
}