package com.devteria.chat.service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.devteria.chat.entity.WebSocketSession;
import com.devteria.chat.repository.WebSocketSessionRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.corundumstudio.socketio.SocketIOServer;
import com.devteria.chat.dto.request.ChatMessageRequest;
import com.devteria.chat.dto.response.ChatMessageResponse;
import com.devteria.chat.entity.ChatMessage;
import com.devteria.chat.entity.ParticipantInfo;
import com.devteria.chat.exception.AppException;
import com.devteria.chat.exception.ErrorCode;
import com.devteria.chat.mapper.ChatMessageMapper;
import com.devteria.chat.repository.ChatMessageRepository;
import com.devteria.chat.repository.ConversationRepository;
import com.devteria.chat.repository.httpclient.ProfileClient;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ChatMessageService {
    SocketIOServer socketIOServer;

    ChatMessageRepository chatMessageRepository;
    ConversationRepository conversationRepository;
    ProfileClient profileClient;
    ObjectMapper objectMapper;
    ChatMessageMapper chatMessageMapper;
    private final WebSocketSessionRepository webSocketSessionRepository;

    public List<ChatMessageResponse> getMessages(String conversationId) {
        // Validate conversationId
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        conversationRepository
                .findById(conversationId)
                .orElseThrow(() -> new AppException(ErrorCode.CONVERSATION_NOT_FOUND))
                .getParticipants()
                .stream()
                .filter(participantInfo -> userId.equals(participantInfo.getUserId()))
                .findAny()
                .orElseThrow(() -> new AppException(ErrorCode.CONVERSATION_NOT_FOUND));

        var messages = chatMessageRepository.findAllByConversationIdOrderByCreatedDateDesc(conversationId);

        return messages.stream().map(this::toChatMessageResponse).toList();
    }

    public ChatMessageResponse create(ChatMessageRequest request) throws JsonProcessingException {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        // Validate conversationId
        var conversation=conversationRepository
                .findById(request.getConversationId())
                .orElseThrow(() -> new AppException(ErrorCode.CONVERSATION_NOT_FOUND));

        conversation.getParticipants()
                .stream()
                .filter(participantInfo -> userId.equals(participantInfo.getUserId()))
                .findAny()
                .orElseThrow(() -> new AppException(ErrorCode.CONVERSATION_NOT_FOUND));

        // Get UserInfo from ProfileService
        var userResponse = profileClient.getProfile(userId);
        if (Objects.isNull(userResponse)) {
            throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION);
        }
        var userInfo = userResponse.getResult();

        // Build Chat message Info
        ChatMessage chatMessage = chatMessageMapper.toChatMessage(request);
        chatMessage.setSender(ParticipantInfo.builder()
                .userId(userInfo.getUserId())
                .username(userInfo.getUsername())
                .firstName(userInfo.getFirstName())
                .lastName(userInfo.getLastName())
                .avatar(userInfo.getAvatar())
                .build());
        chatMessage.setCreatedDate(Instant.now());

        // Create chat message
        chatMessage = chatMessageRepository.save(chatMessage);


        // Publish socket event to clients is conversation
        //Get participants userIds
        List<String> userIds = conversation.getParticipants().stream()
                        .map(ParticipantInfo::getUserId).toList();
        // lấy dữ liệu của Websocketsession dưới dạng map<key,value>
        Map<String,WebSocketSession> webSocketSessions = webSocketSessionRepository
                .findAllByUserIdIn(userIds)
                .stream().collect(Collectors.toMap(WebSocketSession::getSocketSessionId, Function.identity()));

        ChatMessageResponse chatMessageResponse = chatMessageMapper.toChatMessageResponse(chatMessage);
        socketIOServer.getAllClients().forEach(client -> {
            // chỉ lấy ra đối tượng nếu sessionId có trong danh sách webSocketSessions nếu không có sẽ trả null.
            var webSocketSession = webSocketSessions.get(client.getSessionId().toString());
            if(Objects.nonNull(webSocketSession)) {

                try {
                    chatMessageResponse.setMe(webSocketSession.getUserId().equals(userId));
                    String message = objectMapper.writeValueAsString(chatMessageResponse);
                    client.sendEvent("message", message);
                }catch (JsonProcessingException e){
                    throw new RuntimeException(e);
                }

            }

        });

        // convert to Response
        return toChatMessageResponse(chatMessage);
    }

    private ChatMessageResponse toChatMessageResponse(ChatMessage chatMessage) {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        var chatMessageResponse = chatMessageMapper.toChatMessageResponse(chatMessage);

        chatMessageResponse.setMe(userId.equals(chatMessage.getSender().getUserId()));

        return chatMessageResponse;
    }
}
