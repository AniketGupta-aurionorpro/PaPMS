package com.aurionpro.papms.service;

import com.aurionpro.papms.dto.DocumentResponseDto;

public interface DocumentService {

    DocumentResponseDto approveDocument(Integer organizationId, Integer documentId);

    DocumentResponseDto rejectDocument(Integer organizationId, Integer documentId);
}