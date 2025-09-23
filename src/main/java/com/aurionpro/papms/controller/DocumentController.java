package com.aurionpro.papms.controller;

import com.aurionpro.papms.dto.DocumentResponseDto;
import com.aurionpro.papms.service.DocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;

    // Endpoint for Bank Admin to approve a pending document
    @PutMapping("/{id}/approve")
    @PreAuthorize("hasRole('BANK_ADMIN')")
    public ResponseEntity<DocumentResponseDto> approveDocument(@PathVariable("id") Integer documentId) {
        DocumentResponseDto updatedDocument = documentService.approveDocument(documentId);
        return ResponseEntity.ok(updatedDocument);
    }

    // Endpoint for Bank Admin to reject a pending document
    @PutMapping("/{id}/reject")
    @PreAuthorize("hasRole('BANK_ADMIN')")
    public ResponseEntity<DocumentResponseDto> rejectDocument(@PathVariable("id") Integer documentId) {
        DocumentResponseDto updatedDocument = documentService.rejectDocument(documentId);
        return ResponseEntity.ok(updatedDocument);
    }
}