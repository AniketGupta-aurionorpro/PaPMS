// mapper/DocumentMapper.java
package com.aurionpro.papms.mapper;

import com.aurionpro.papms.dto.DocumentResponseDto;
import com.aurionpro.papms.entity.Document;

public class DocumentMapper {
    public static DocumentResponseDto fromEntity(Document document) {
        return DocumentResponseDto.builder()
                .id(document.getId())
                .fileName(document.getFileName())
                .url(document.getCloudinaryUrl())
                .type(document.getRelatedEntityType())
                .status(document.getStatus())
                .uploadedAt(document.getUploadedAt())
                .build();
    }
}