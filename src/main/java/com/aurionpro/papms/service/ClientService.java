package com.aurionpro.papms.service;

import com.aurionpro.papms.dto.*;

import java.util.List;

public interface ClientService {
    // Client Management
    ClientResponseDto createClient(ClientRequestDto request);
    ClientResponseDto getClientById(Integer clientId);
    List<ClientResponseDto> getAllClientsForCurrentOrg();
    ClientResponseDto updateClient(Integer clientId, ClientRequestDto request);
    void toggleClientStatus(Integer clientId, boolean isActive);

    // Invoice Management
    InvoiceResponseDto createInvoice(InvoiceRequestDto request);
    InvoiceResponseDto getInvoiceById(Integer invoiceId);
    List<InvoiceResponseDto> getAllInvoicesForClient(Integer clientId);
    List<InvoiceResponseDto> getAllInvoicesForCurrentOrg();

    // Payment Processing
    String processInvoicePayment(Integer invoiceId);
}