package com.aurionpro.papms.mapper;

import com.aurionpro.papms.dto.InvoiceRequestDto;
import com.aurionpro.papms.dto.InvoiceResponseDto;
import com.aurionpro.papms.entity.Client;
import com.aurionpro.papms.entity.Invoice;
import com.aurionpro.papms.entity.Organization;

public class InvoiceMapper {

    /**
     * Converts an Invoice entity to an InvoiceResponseDto.
     */
    public static InvoiceResponseDto toDto(Invoice invoice) {
        return InvoiceResponseDto.builder()
                .id(invoice.getId())
                .invoiceNumber(invoice.getInvoiceNumber())
                .amount(invoice.getAmount())
                .issueDate(invoice.getIssueDate())
                .dueDate(invoice.getDueDate())
                .status(invoice.getStatus())
                .paidAt(invoice.getPaidAt())
                .organizationId(invoice.getOrganization().getId())
                .organizationName(invoice.getOrganization().getCompanyName())
                .clientId(invoice.getClient().getId())
                .clientCompanyName(invoice.getClient().getCompanyName())
                .clientContactPerson(invoice.getClient().getContactPerson())
                .build();
    }

    /**
     * Converts an InvoiceRequestDto to an Invoice entity.
     */
    public static Invoice toEntity(InvoiceRequestDto dto, Organization organization, Client client) {
        return Invoice.builder()
                .invoiceNumber(dto.getInvoiceNumber())
                .amount(dto.getAmount())
                .issueDate(dto.getIssueDate())
                .dueDate(dto.getDueDate())
                .organization(organization)
                .client(client)
                .build();
    }
}