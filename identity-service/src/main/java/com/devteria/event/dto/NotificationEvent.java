package com.devteria.event.dto;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class NotificationEvent {
    String channel;
    String recipient;
    String templateCode;
    Map<String, Object> param;
    String subject;
    String body;
}
