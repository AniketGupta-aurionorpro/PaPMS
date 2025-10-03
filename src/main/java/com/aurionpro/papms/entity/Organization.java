package com.aurionpro.papms.entity;

import com.aurionpro.papms.Enum.OrganizationStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import com.fasterxml.jackson.annotation.JsonManagedReference;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "organizations")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Organization {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "company_name", nullable = false, unique = true)
    private String companyName;

    @Column(columnDefinition = "TEXT")
    private String address;

    @Column(name = "contact_email", nullable = false)
    private String contactEmail;

    @Column(name = "contact_phone")
    private String contactPhone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private OrganizationStatus status = OrganizationStatus.PENDING_APPROVAL;

    @Column(name = "bank_assigned_account_number", unique = true, length = 50)
    private String bankAssignedAccountNumber;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @Column(name = "internal_balance", nullable = false)
    @Builder.Default
    private BigDecimal internalBalance = BigDecimal.ZERO;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "organization", fetch = FetchType.EAGER)
    private List<Employee> employees;

    @OneToMany(mappedBy = "organization", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @org.hibernate.annotations.Where(clause = "related_entity_type = 'ORGANIZATION_VERIFICATION'")
    @JsonManagedReference
    private List<Document> documents;

    @Column(name = "logo_url", length = 512) // ADD THIS LINE
    private String logoUrl;
}