package com.aurionpro.papms.entity;

import com.aurionpro.papms.Enum.DocumentStatus;
import com.aurionpro.papms.Enum.DocumentType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "documents")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private String fileName;

    @Column(nullable = false, length = 512)
    private String cloudinaryUrl;

    @Column(nullable = false)
    private String cloudinaryPublicId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DocumentType relatedEntityType;

    @Column(nullable = false)
    private Integer relatedEntityId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DocumentStatus status;

    @Column(nullable = false, updatable = false)
    private LocalDateTime uploadedAt = LocalDateTime.now();
}