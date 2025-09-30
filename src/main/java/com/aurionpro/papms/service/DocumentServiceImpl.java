package com.aurionpro.papms.service;

import com.aurionpro.papms.Enum.DocumentStatus;
import com.aurionpro.papms.Enum.DocumentType;
import com.aurionpro.papms.dto.DocumentResponseDto;
import com.aurionpro.papms.entity.Document;
import com.aurionpro.papms.exception.NotFoundException;
import com.aurionpro.papms.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class DocumentServiceImpl implements DocumentService {

    private final DocumentRepository documentRepository;

    // CHANGED: Method signature updated.
    @Override
    public DocumentResponseDto approveDocument(Integer organizationId, Integer documentId) {
        // 1. Find the document by its own ID first.
        Document document = findDocumentById(documentId);

        // 2. NEW VALIDATION: Ensure the found document belongs to the correct organization and type.
        validateDocumentOwnership(document, organizationId, documentId);

        // 3. Existing validation: Ensure the document is in a state that can be approved.
        if (document.getStatus() != DocumentStatus.Pending) {
            throw new IllegalStateException("Only pending documents can be approved. Current status: " + document.getStatus());
        }

        // 4. Proceed with approval.
        document.setStatus(DocumentStatus.Approved);
        Document updatedDocument = documentRepository.save(document);
        return DocumentResponseDto.fromEntity(updatedDocument);
    }

    // CHANGED: Method signature updated.
    @Override
    public DocumentResponseDto rejectDocument(Integer organizationId, Integer documentId) {
        // 1. Find the document.
        Document document = findDocumentById(documentId);

        // 2. NEW VALIDATION: Ensure ownership.
        validateDocumentOwnership(document, organizationId, documentId);

        // 3. Existing validation: Check status.
        if (document.getStatus() != DocumentStatus.Pending) {
            throw new IllegalStateException("Only pending documents can be rejected. Current status: " + document.getStatus());
        }

        // 4. Proceed with rejection.
        document.setStatus(DocumentStatus.Rejected);
        Document updatedDocument = documentRepository.save(document);
        return DocumentResponseDto.fromEntity(updatedDocument);
    }

    private Document findDocumentById(Integer documentId) {
        return documentRepository.findById(documentId)
                .orElseThrow(() -> new NotFoundException("Document not found with ID: " + documentId));
    }

    // NEW HELPER METHOD: Contains the reusable validation logic.
//    private void validateDocumentOwnership(Document document, Integer organizationId, Integer documentId) {
//        // Check if the document's related entity ID matches the organization ID from the URL.
//        if (!document.getOrganization().getId().equals(organizationId)) {
//            throw new IllegalArgumentException("Access Denied: Document with ID " + documentId + " does not belong to organization with ID " + organizationId);
//        }
//
//        // Optional but recommended: Check if this is actually an organization verification document.
//        if (document.getRelatedEntityType() != DocumentType.ORGANIZATION_VERIFICATION) {
//            throw new IllegalArgumentException("Invalid Action: Document with ID " + documentId + " is not an organization verification document.");
//        }
//    }
    private void validateDocumentOwnership(Document document, Integer organizationId, Integer documentId) {
        // Check if the document's organization object exists and its ID matches the one from the URL.
        if (document.getOrganization() == null || !document.getOrganization().getId().equals(organizationId)) {
            throw new IllegalArgumentException("Access Denied: Document with ID " + documentId + " does not belong to organization with ID " + organizationId);
        }

        // Optional but recommended: Check if this is actually an organization verification document.
        if (document.getRelatedEntityType() != DocumentType.ORGANIZATION_VERIFICATION) {
            throw new IllegalArgumentException("Invalid Action: Document with ID " + documentId + " is not an organization verification document.");
        }
    }
}