package com.devteria.chat.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.devteria.chat.entity.WebSocketSession;

import java.util.List;

@Repository
public interface WebSocketSessionRepository extends MongoRepository<WebSocketSession, String> {
    void deleteBySocketSessionId(String socketSessionId);
    List<WebSocketSession> findAllByUserIdIn(List<String> userIds);
}
