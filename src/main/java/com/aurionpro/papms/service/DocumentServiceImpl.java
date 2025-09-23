package com.aurionpro.papms.service;

import com.aurionpro.papms.Enum.DocumentStatus;
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

    @Override
    public DocumentResponseDto approveDocument(Integer documentId) {
        Document document = findDocumentById(documentId);

        if (document.getStatus() != DocumentStatus.Pending) {
            throw new IllegalStateException("Only pending documents can be approved. Current status: " + document.getStatus());
        }

        document.setStatus(DocumentStatus.Approved);
        Document updatedDocument = documentRepository.save(document);
        return DocumentResponseDto.fromEntity(updatedDocument);
    }

    @Override
    public DocumentResponseDto rejectDocument(Integer documentId) {
        Document document = findDocumentById(documentId);

        if (document.getStatus() != DocumentStatus.Pending) {
            throw new IllegalStateException("Only pending documents can be rejected. Current status: " + document.getStatus());
        }

        document.setStatus(DocumentStatus.Rejected);
        Document updatedDocument = documentRepository.save(document);
        return DocumentResponseDto.fromEntity(updatedDocument);
    }

    private Document findDocumentById(Integer documentId) {
        return documentRepository.findById(documentId)
                .orElseThrow(() -> new NotFoundException("Document not found with ID: " + documentId));
    }
}