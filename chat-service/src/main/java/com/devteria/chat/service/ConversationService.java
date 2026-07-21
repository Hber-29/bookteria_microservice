package com.devteria.chat.service;

import com.devteria.chat.dto.ApiResponse;
import com.devteria.chat.dto.request.ConversationRequest;
import com.devteria.chat.dto.response.ConversationResponse;
import com.devteria.chat.entity.Conversation;
import com.devteria.chat.entity.ParticipantInfo;
import com.devteria.chat.exception.AppException;
import com.devteria.chat.exception.ErrorCode;
import com.devteria.chat.mapper.ConversationMapper;
import com.devteria.chat.repository.ConversationRepository;
import com.devteria.chat.repository.httpclient.ProfileClient;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ConversationService {
    ConversationRepository conversationRepository;
    ProfileClient profileClient;
    ConversationMapper conversationMapper;

    public List<ConversationResponse> myConversations() {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        List<Conversation> convertations= conversationRepository.findAllByParticipantIdsContains(userId);
        return convertations.stream().map(this::toConversationResponse).toList();
    }

    public ConversationResponse create(ConversationRequest request) {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        var userInforResponse = profileClient.getProfile(userId);
        var patricipantInforResponse = profileClient.getProfile(request.getParticipantIds().getFirst());
        if(Objects.isNull(userInforResponse) || Objects.isNull(patricipantInforResponse)) {
            throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION);
        }
        var userInfor= userInforResponse.getResult();
        var patricipantInfor = patricipantInforResponse.getResult();
        List<String> userIds = new ArrayList<>();
        userIds.add(userId);
        userIds.add(patricipantInfor.getId());
        var sortedIds= userIds.stream().sorted().toList();
        String userHash = generateParticipantHash(sortedIds);

        Conversation conversation= conversationRepository.findByParticipantsHash(userHash)
                .orElseGet(()->{
                    List<ParticipantInfo> participantinfos = List.of(
                            ParticipantInfo.builder()
                                    .userId(userInfor.getId())
                                    .username(userInfor.getUsername())
                                    .firstName(userInfor.getFirstName())
                                    .lastName(userInfor.getLastName())
                                    .avatar(userInfor.getAvatar())
                                    .build(),

                            ParticipantInfo.builder()
                                    .userId(patricipantInfor.getId())
                                    .username(patricipantInfor.getUsername())
                                    .firstName(patricipantInfor.getFirstName())
                                    .lastName(patricipantInfor.getLastName())
                                    .avatar(patricipantInfor.getAvatar())
                                    .build()


                    );

                    Conversation newConversation = Conversation.builder()
                            .type(request.getType())
                            .participantsHash(userHash)
                            .createdDate(Instant.now())
                            .modifiedDate(Instant.now())
                            .participants(participantinfos)
                            .build();

                    return conversationRepository.save(newConversation);


                } );

        return toConversationResponse(conversation) ;
    }

    private String generateParticipantHash(List<String> ids) {
        StringJoiner stringJoiner = new StringJoiner("_");
        ids.forEach(stringJoiner::add);
        return stringJoiner.toString();
    }

    // đây là hàm mapper cũng đồng thời tùy biến dữ liệu hiển thị theo góc nhìn của người đăng nhập.

    private ConversationResponse toConversationResponse(Conversation conversation) {
        String currentUserId = SecurityContextHolder.getContext().getAuthentication().getName();

        ConversationResponse conversationResponse = conversationMapper.toConversationResponse(conversation);

        conversation.getParticipants().stream()
                // sẽ giữ lại các userId khác với current id vì chỉ lấy dữ liệu của đối phương làm tên đoạn chat 1-1
                .filter(participantInfo -> !participantInfo.getUserId().equals(currentUserId))
                .findFirst().ifPresent(participantInfo -> {
                    conversationResponse.setConversationName(participantInfo.getUsername());
                    conversationResponse.setConversationAvatar(participantInfo.getAvatar());
                });

        return conversationResponse;
    }
}
