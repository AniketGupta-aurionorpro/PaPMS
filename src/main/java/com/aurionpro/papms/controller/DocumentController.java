package com.aurionpro.papms.controller;

import com.aurionpro.papms.dto.DocumentResponseDto;
import com.aurionpro.papms.service.DocumentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/organizations/{organizationId}/documents")
@RequiredArgsConstructor
@Slf4j
public class DocumentController {

    private final DocumentService documentService;

    // Endpoint for Bank Admin to approve a pending document
    @PutMapping("/{documentId}/approve")
    @PreAuthorize("hasRole('BANK_ADMIN')")
    public ResponseEntity<DocumentResponseDto> approveDocument(
            @PathVariable("organizationId") Integer organizationId,
            @PathVariable("documentId") Integer documentId) {
        log.info("Request to APPROVE document ID {} for organization ID {}", documentId, organizationId);
        DocumentResponseDto updatedDocument = documentService.approveDocument(organizationId, documentId);
        log.info("Successfully approved document ID {}. New status: {}", documentId, updatedDocument.getStatus());
        return ResponseEntity.ok(updatedDocument);
    }

    // Endpoint for Bank Admin to reject a pending document
    @PutMapping("/{documentId}/reject")
    @PreAuthorize("hasRole('BANK_ADMIN')")
    public ResponseEntity<DocumentResponseDto> rejectDocument(
            @PathVariable("organizationId") Integer organizationId,
            @PathVariable("documentId") Integer documentId) {
        log.info("Request to REJECT document ID {} for organization ID {}", documentId, organizationId);
        DocumentResponseDto updatedDocument = documentService.rejectDocument(organizationId, documentId);
        log.info("Successfully rejected document ID {}. New status: {}", documentId, updatedDocument.getStatus());
        return ResponseEntity.ok(updatedDocument);
    }
}