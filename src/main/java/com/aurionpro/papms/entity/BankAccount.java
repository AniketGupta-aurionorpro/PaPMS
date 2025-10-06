package com.aurionpro.papms.entity;

import com.aurionpro.papms.Enum.OwnerType;
import com.aurionpro.papms.entity.vendorEntity.Vendor;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "bank_accounts")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BankAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // For Employees
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Employee employee;

    // For Vendors
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Vendor vendor;

    @Enumerated(EnumType.STRING)
    @Column(name = "owner_type", nullable = false)
    private OwnerType ownerType;

    @Column(name = "account_holder_name", nullable = false)
    private String accountHolderName;

    @Column(name = "account_number", nullable = false, length = 50)
    private String accountNumber;

    @Column(name = "bank_name", nullable = false)
    private String bankName;

    @Column(name = "ifsc_code", nullable = false, length = 11)
    private String ifscCode;

    @Column(name = "is_primary")
    @Builder.Default
    private boolean isPrimary = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // Helper methods remain the same
    public Long getOwnerId() {
        return ownerType == OwnerType.EMPLOYEE ?
                (employee != null ? employee.getId() : null) :
                (vendor != null ? vendor.getId() : null);
    }

    public String getOwnerName() {
        if (ownerType == OwnerType.EMPLOYEE && employee != null && employee.getUser() != null) {
            return employee.getUser().getFullName();
        } else if (ownerType == OwnerType.VENDOR && vendor != null) {
            return vendor.getVendorName();
        }
        return null;
    }
}