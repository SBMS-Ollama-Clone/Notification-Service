package com.kkimleang.notificationservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.mail.javamail.MimeMessagePreparator;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {
    private final JavaMailSender javaMailSender;

    @KafkaListener(topics = "chat-deleted")
    public void listen(com.kkimleang.chatservice.event.ChatDeletedEvent chatDeletedEvent) {
        MimeMessagePreparator messagePreparator = mimeMessage -> {
            MimeMessageHelper messageHelper = new MimeMessageHelper(mimeMessage);
            messageHelper.setFrom("ollama.service@service.com");
            messageHelper.setTo(chatDeletedEvent.getEmail());
            messageHelper.setText(String.format("""
                                    Hi %s,
                                    
                                    Your chat with title %s has been deleted.
                                    Id: %s
                                    
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
}