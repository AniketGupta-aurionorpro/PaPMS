package com.aurionpro.papms.service;

import com.aurionpro.papms.dto.DocumentResponseDto;

public interface DocumentService {

    DocumentResponseDto approveDocument(Integer documentId);

    DocumentResponseDto rejectDocument(Integer documentId);
}