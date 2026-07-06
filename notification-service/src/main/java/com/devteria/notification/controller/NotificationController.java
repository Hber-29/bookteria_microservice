package com.devteria.notification.controller;

import com.devteria.event.dto.NotificationEvent;
import com.devteria.notification.dto.request.Recipient;
import com.devteria.notification.dto.request.SendEmailRequest;
import com.devteria.notification.service.EmailService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;


@Component
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class NotificationController {
    EmailService emailService;


    @KafkaListener(topics = "notification-delivery")
    public void listen(NotificationEvent event){
        log.info("Message received: {}", event);
        emailService.sendEmail(SendEmailRequest.builder()
                        .to(Recipient.builder()
                                .email(event.getRecipient())
                                .build())
                        .subject(event.getSubject())
                        .htmlContent(event.getBody())
                .build());
    }
}
