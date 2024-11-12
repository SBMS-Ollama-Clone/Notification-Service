package com.kkimleang.notificationservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.*;
import org.springframework.kafka.support.*;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.mail.javamail.MimeMessagePreparator;
import org.springframework.messaging.handler.annotation.*;
import org.springframework.retry.annotation.*;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {
    private final JavaMailSender javaMailSender;

    @RetryableTopic(attempts = "2", backoff = @Backoff(delay = 1000, multiplier = 1.5, maxDelay = 1500))
    @KafkaListener(topics = "chat-deleted")
    public void listen(com.kkimleang.chatservice.event.ChatDeletedEvent chatDeletedEvent, @Header(KafkaHeaders.RECEIVED_TOPIC) String topic, @Header(KafkaHeaders.OFFSET) long offset) {
        log.info("Received chat-deleted event: {}, {}, {}", chatDeletedEvent, topic, offset);
        MimeMessagePreparator messagePreparator = mimeMessage -> {
            MimeMessageHelper messageHelper = new MimeMessageHelper(mimeMessage);
            messageHelper.setFrom("ollama.service@service.com");
            messageHelper.setTo(chatDeletedEvent.getEmail());
            messageHelper.setText(String.format("""
                                    Hi %s,
                                    <br/>
                                    Your chat with title %s has been deleted.
                                    Id: %s
                                    <br/>
                                    Best Regards
                                    Ollama Service Team
                                    """,
                            chatDeletedEvent.getUsername(),
                            chatDeletedEvent.getTitle(),
                            chatDeletedEvent.getId()),
                    true
            );
        };
        try {
            javaMailSender.send(messagePreparator);
        } catch (MailException e) {
            throw new RuntimeException("Exception occurred when sending mail to ollama.service@service.com", e);
        }
    }

    @Recover
    public void recover(RuntimeException e, com.kkimleang.chatservice.event.ChatDeletedEvent chatDeletedEvent) {
        log.error("Failed to send email to {} Recover", chatDeletedEvent.getEmail());
    }

    @DltHandler
    public void listenDLT(com.kkimleang.chatservice.event.ChatDeletedEvent chatDeletedEvent, @Header(KafkaHeaders.RECEIVED_TOPIC) String topic, @Header(KafkaHeaders.OFFSET) long offset) {
        log.error("Failed to send email to {} DLT", chatDeletedEvent.getEmail());
    }
}