package com.aurionpro.papms.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

import com.aurionpro.papms.Enum.OrganizationStatus;

@Entity
@Table(name = "organizations") // Maps this entity to the 'organizations' table
@Data // Generates getters, setters, toString(), equals(), and hashCode()
@NoArgsConstructor // Creates a no-args constructor
@AllArgsConstructor // Creates a constructor with all fields
@Builder // Provides a builder pattern for creating instances
public class Organization {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id; // The primary key for the organization

    @Column(nullable = false, unique = true)
    private String companyName; // The unique name of the organization

    @Column(nullable = false, unique = true)
    private String username; // Username for the organization's admin

    @Column(nullable = false)
    private String password; // Hashed password for the organization admin

    private String fullName; // Full name of the organization admin

    @Column(nullable = false, unique = true)
    @Email
    private String email; // The primary contact email for the organization

    @Column(columnDefinition = "TEXT")
    private String address; // The physical address of the organization

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrganizationStatus status = OrganizationStatus.PENDING_APPROVAL; // The current status of the organization

    @Column(unique = true, length = 50)
    private String bankAssignedAccountNumber; // Bank account number assigned by the bank

    @Column(columnDefinition = "TEXT")
    private String rejectionReason; // Reason for rejection, if applicable

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now(); // Timestamp of creation

    // You will also need to create the OrganizationStatus enum to map to the ENUM in your DB
}