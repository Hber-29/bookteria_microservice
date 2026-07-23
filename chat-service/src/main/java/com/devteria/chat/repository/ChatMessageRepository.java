package com.devteria.chat.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.devteria.chat.entity.ChatMessage;

@Repository
public interface ChatMessageRepository extends MongoRepository<ChatMessage, String> {
    // hàm lấy danh sách các message theo conversationid theo thứ tự giảm dần.
    List<ChatMessage> findAllByConversationIdOrderByCreatedDateDesc(String conversationId);
}
