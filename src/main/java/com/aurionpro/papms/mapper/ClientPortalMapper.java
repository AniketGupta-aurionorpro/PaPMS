package com.aurionpro.papms.mapper;

import com.aurionpro.papms.dto.ClientDto;
import com.aurionpro.papms.entity.Client;

/**
 * Mapper for Client entity to DTO conversions
 */
public class ClientPortalMapper {

    public static ClientDto toDto(Client client) {
        if (client == null)
            return null;

        return ClientDto.builder()
                .id(client.getId())
                .userId(client.getUser().getId())
                .username(client.getUser().getUsername())
                .organizationId(client.getOrganization().getId())
                .organizationName(client.getOrganization().getCompanyName())
                .clientName(client.getClientName())
                .contactEmail(client.getContactEmail())
                .contactPhone(client.getContactPhone())
                .address(client.getAddress())
                .balance(client.getBalance())
                .status(client.getStatus().name())
                .createdAt(client.getCreatedAt())
                .updatedAt(client.getUpdatedAt())
                .build();
    }
}
