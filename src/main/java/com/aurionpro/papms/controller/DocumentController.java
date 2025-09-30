package com.aurionpro.papms.controller;

import com.aurionpro.papms.dto.DocumentResponseDto;
import com.aurionpro.papms.service.DocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/organizations/{organizationId}/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;

    // Endpoint for Bank Admin to approve a pending document
    @PutMapping("/{documentId}/approve")
    @PreAuthorize("hasRole('BANK_ADMIN')")
    public ResponseEntity<DocumentResponseDto> approveDocument(
            @PathVariable("organizationId") Integer organizationId,
            @PathVariable("documentId") Integer documentId) {
        // CHANGED: Pass both IDs to the service layer for validation.
        DocumentResponseDto updatedDocument = documentService.approveDocument(organizationId, documentId);
        return ResponseEntity.ok(updatedDocument);
    }

    // Endpoint for Bank Admin to reject a pending document
    @PutMapping("/{documentId}/reject")
    @PreAuthorize("hasRole('BANK_ADMIN')")
    public ResponseEntity<DocumentResponseDto> rejectDocument(
            @PathVariable("organizationId") Integer organizationId,
            @PathVariable("documentId") Integer documentId) {
        // CHANGED: Pass both IDs to the service layer.
        DocumentResponseDto updatedDocument = documentService.rejectDocument(organizationId, documentId);
        return ResponseEntity.ok(updatedDocument);
    }
}