package com.aurionpro.papms.mapper;

import com.aurionpro.papms.Enum.Role;
import com.aurionpro.papms.dto.ClientRequestDto;
import com.aurionpro.papms.dto.ClientResponseDto;
import com.aurionpro.papms.entity.Client;
import com.aurionpro.papms.entity.Organization;
import com.aurionpro.papms.entity.User;

public class ClientMapper {

    /**
     * Creates a User entity from a ClientRequestDto.
     */
    public static User toUserEntity(ClientRequestDto dto, Organization organization, String encodedPassword) {
        return User.builder()
                .username(dto.getUsername())
                .password(encodedPassword)
                .fullName(dto.getFullName())
                .email(dto.getEmail())
                .role(Role.CLIENT)
                .organizationId(organization.getId())
                .isActive(true)
                .requiresPasswordChange(true)
                .build();
    }

    /**
     * Creates a Client entity from a ClientRequestDto.
     */
    public static Client toClientEntity(ClientRequestDto dto, User user, Organization organization) {
        return Client.builder()
                .user(user)
                .organization(organization)
                .companyName(dto.getCompanyName())
                .contactPerson(dto.getContactPerson())
                .isActive(true)
                .build();
    }

    /**
     * Converts a Client entity to a ClientResponseDto.
     */
    public static ClientResponseDto toDto(Client client) {
        User user = client.getUser();
        Organization org = client.getOrganization();
        return ClientResponseDto.builder()
                .clientId(client.getId())
                .companyName(client.getCompanyName())
                .contactPerson(client.getContactPerson())
                .isClientActive(client.isActive())
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .isUserActive(user.getIsActive())
                .organizationId(org.getId())
                .organizationName(org.getCompanyName())
                .createdAt(client.getCreatedAt())
                .build();
    }
}