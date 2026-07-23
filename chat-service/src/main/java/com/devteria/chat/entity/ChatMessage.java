package com.devteria.chat.entity;

import java.time.Instant;

import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.MongoId;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "chat_message")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ChatMessage {
    @MongoId
    String id;

    // để cho mã hash không được lặp lại điều này tránh việc tạo ra 2 đoạn hội thoại giống nhau
    @Indexed
    String conversationId;

    String message;

    ParticipantInfo sender;

    // sắp xếp ngày tạo theo index để có thể lấy đoạn chat mới nhất theo thứ tự
    @Indexed
    Instant createdDate;
}
