package com.aurionpro.papms.controller;

import com.aurionpro.papms.dto.DocumentResponseDto;
import com.aurionpro.papms.entity.Document;
import com.aurionpro.papms.repository.DocumentRepository;
import com.aurionpro.papms.service.DocumentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

@RestController
@RequestMapping("/api/organizations/{organizationId}/documents")
@RequiredArgsConstructor
@Slf4j
public class DocumentController {

    private final DocumentService documentService;
    private final DocumentRepository documentRepository;
    private final RestTemplate restTemplate;

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

    // NEW: Proxy endpoint to stream document content (bypasses Cloudinary 401)
    @GetMapping("/{documentId}/content")
    @PreAuthorize("hasRole('BANK_ADMIN') or hasRole('ORG_ADMIN')")
    public ResponseEntity<byte[]> getDocumentContent(
            @PathVariable("organizationId") Integer organizationId,
            @PathVariable("documentId") Integer documentId) {
        log.info("Request to fetch document content for document ID {} in organization {}", documentId, organizationId);

        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found"));

        if (!document.getOrganization().getId().equals(organizationId)) {
            throw new RuntimeException("Document does not belong to this organization");
        }

        String cloudinaryUrl = document.getCloudinaryUrl();
        if (cloudinaryUrl == null || cloudinaryUrl.isEmpty()) {
            throw new RuntimeException("Document URL not available");
        }

        try {
            // Fetch the document from Cloudinary
            ResponseEntity<byte[]> response = restTemplate.getForEntity(cloudinaryUrl, byte[].class);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDisposition(ContentDisposition.inline().filename(document.getFileName()).build());

            log.info("Successfully proxied document content for document ID {}", documentId);
            return new ResponseEntity<>(response.getBody(), headers, HttpStatus.OK);
        } catch (Exception e) {
            log.error("Failed to fetch document content from Cloudinary: {}", e.getMessage());
            throw new RuntimeException("Failed to fetch document content");
        }
    }
}