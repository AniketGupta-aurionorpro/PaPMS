package com.aurionpro.papms.dto;

import java.util.Set;

import com.aurionpro.papms.Enum.Role;
import com.aurionpro.papms.entity.User;

public record RegisterRequest(String username, String password, String fullName,String email,Role role, int organizationId, boolean enable) {
    public static RegisterRequest from(User u) {
        return new RegisterRequest(u.getUsername(), u.getPassword(),u.getFullName(),u.getEmail(),u.getRole(),u.getOrganizationId(),u.getEnable());
    }
}