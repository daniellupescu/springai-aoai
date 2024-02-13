package com.example.springai.aoai.mapper;

import com.example.springai.aoai.config.SpringMapperConfig;
import com.example.springai.aoai.dto.ChatMessageDto;
import org.mapstruct.Mapper;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.UserMessage;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper for {@link org.springframework.ai.chat.messages.Message}
 */
@Mapper(config = SpringMapperConfig.class)
public abstract class ChatMessageMapper {
    /**
     * Convert {@link com.example.springai.aoai.dto.ChatMessageDto} to {@link org.springframework.ai.chat.messages.Message}
     *
     * @param chatMessageDto chatMessageDto to convert
     * @return Message instance of AssistantMessage or UserMessage
     */
    public Message toMessage(ChatMessageDto chatMessageDto) {
        if (MessageType.ASSISTANT.getValue().equalsIgnoreCase(chatMessageDto.getRole())) {
            return new AssistantMessage(chatMessageDto.getContent());
        } else if (MessageType.USER.getValue().equalsIgnoreCase(chatMessageDto.getRole())) {
            return new UserMessage(chatMessageDto.getContent());
        } else {
            throw new RuntimeException("Invalid message type");
        }
    }

    /**
     * Convert list of {@link com.example.springai.aoai.dto.ChatMessageDto} to list of {@link org.springframework.ai.chat.messages.Message}
     *
     * @param chatMessageDtos list of chatMessageDto to convert
     * @return list of {@link org.springframework.ai.chat.messages.Message}
     */
    public List<Message> toMessage(List<ChatMessageDto> chatMessageDtos) {
        return chatMessageDtos.stream()
                .map(chatMessageDto -> {
                    return toMessage(chatMessageDto);
                })
                .collect(Collectors.toList());
    }
}
