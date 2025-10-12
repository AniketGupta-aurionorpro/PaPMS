package com.aurionpro.papms.service;

import com.aurionpro.papms.Enum.DocumentStatus;
import com.aurionpro.papms.Enum.DocumentType;
import com.aurionpro.papms.dto.DocumentResponseDto;
import com.aurionpro.papms.entity.Document;
import com.aurionpro.papms.exception.NotFoundException;
import com.aurionpro.papms.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class DocumentServiceImpl implements DocumentService {

    private final DocumentRepository documentRepository;

    @Override
    public DocumentResponseDto approveDocument(Integer organizationId, Integer documentId) {
        log.info("Attempting to approve document ID {} for organization ID {}", documentId, organizationId);
        Document document = findDocumentById(documentId);
        validateDocumentOwnership(document, organizationId, documentId);

        if (document.getStatus() != DocumentStatus.Pending) {
            log.warn("Approval failed for document {}: Not in PENDING state. Current state: {}", documentId, document.getStatus());
            throw new IllegalStateException("Only pending documents can be approved. Current status: " + document.getStatus());
        }

        document.setStatus(DocumentStatus.Approved);
        Document updatedDocument = documentRepository.save(document);
        log.info("Document {} approved successfully.", documentId);
        return DocumentResponseDto.fromEntity(updatedDocument);
    }

    @Override
    public DocumentResponseDto rejectDocument(Integer organizationId, Integer documentId) {
        log.info("Attempting to reject document ID {} for organization ID {}", documentId, organizationId);
        Document document = findDocumentById(documentId);
        validateDocumentOwnership(document, organizationId, documentId);

        if (document.getStatus() != DocumentStatus.Pending) {
            log.warn("Rejection failed for document {}: Not in PENDING state. Current state: {}", documentId, document.getStatus());
            throw new IllegalStateException("Only pending documents can be rejected. Current status: " + document.getStatus());
        }

        document.setStatus(DocumentStatus.Rejected);
        Document updatedDocument = documentRepository.save(document);
        log.info("Document {} rejected successfully.", documentId);
        return DocumentResponseDto.fromEntity(updatedDocument);
    }

    private Document findDocumentById(Integer documentId) {
        return documentRepository.findById(documentId)
                .orElseThrow(() -> new NotFoundException("Document not found with ID: " + documentId));
    }

    private void validateDocumentOwnership(Document document, Integer organizationId, Integer documentId) {
        if (document.getOrganization() == null || !document.getOrganization().getId().equals(organizationId)) {
            log.error("SECURITY ALERT: Attempt to access document {} which does not belong to organization {}", documentId, organizationId);
            throw new IllegalArgumentException("Access Denied: Document with ID " + documentId + " does not belong to organization with ID " + organizationId);
        }

        if (document.getRelatedEntityType() != DocumentType.ORGANIZATION_VERIFICATION) {
            log.warn("Invalid action on document {}: Not an organization verification document.", documentId);
            throw new IllegalArgumentException("Invalid Action: Document with ID " + documentId + " is not an organization verification document.");
        }
    }
}