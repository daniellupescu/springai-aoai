package com.example.springai.aoai.dto;

/**
 * DTO class representing a message sent by the client, either with role "user" or "assistant"
 */
public class ChatMessageDto {

    private String content;
    private String role;

    public ChatMessageDto() {
    }

    public ChatMessageDto(String role, String content) {
        this.role = role;
        this.content = content;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}
