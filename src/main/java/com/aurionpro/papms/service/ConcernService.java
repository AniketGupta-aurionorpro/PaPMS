package com.aurionpro.papms.service;
import com.aurionpro.papms.dto.ConcernResponseDto;
import com.aurionpro.papms.dto.RaiseConcernRequest;
import com.aurionpro.papms.dto.UpdateConcernStatusRequest;
import java.util.List;
public interface ConcernService {
    ConcernResponseDto raiseConcern(RaiseConcernRequest request);
    List<ConcernResponseDto> getMyConcerns();
    List<ConcernResponseDto> getConcernsForOrganization(Integer organizationId);
    ConcernResponseDto getConcernById(Long concernId);
    ConcernResponseDto updateConcernStatus(Long concernId, UpdateConcernStatusRequest request);
}