package com.aurionpro.papms.dto;

import com.aurionpro.papms.Enum.DocumentStatus;
import com.aurionpro.papms.Enum.DocumentType;
import com.aurionpro.papms.entity.Document;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class DocumentResponseDto {

    private Integer id;
    private String fileName;
    private String url;
    private DocumentType type;
    private DocumentStatus status;
    private LocalDateTime uploadedAt;

    // A static factory method to map from the entity to the DTO
    public static DocumentResponseDto fromEntity(Document document) {
        return DocumentResponseDto.builder()
                .id(document.getId())
                .fileName(document.getFileName()) // The filename is already sanitized in the service
                .url(document.getCloudinaryUrl())
                .type(document.getRelatedEntityType())
                .status(document.getStatus())
                .uploadedAt(document.getUploadedAt())
                .build();
    }
}