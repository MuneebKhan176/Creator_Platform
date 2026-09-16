// src/main/java/com/application/comment/dtos/CreateCommentRequest.java
package com.application.comment.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CreateCommentRequest {

    @NotBlank(message = "Comment content cannot be empty.")
    @Size(max = 1000, message = "Comment must be 1000 characters or fewer.")
    private String content;

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
}