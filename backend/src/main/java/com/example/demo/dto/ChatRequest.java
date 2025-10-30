package com.example.demo.dto;

import lombok.Data;

@Data
public class ChatRequest {
    private Long userId;
    private String action;
    private String sessionId;
    private String message;
    private Long targetUserId;
    private String fileId; // ID файла в Telegram
    private String fileType; // "photo", "document", "voice", etc.
    private String caption;
    public String getFileId() { return fileId; }
    public void setFileId(String fileId) { this.fileId = fileId; }

    public String getFileType() { return fileType; }
    public void setFileType(String fileType) { this.fileType = fileType; }

    public String getCaption() { return caption; }
    public void setCaption(String caption) { this.caption = caption; }
}