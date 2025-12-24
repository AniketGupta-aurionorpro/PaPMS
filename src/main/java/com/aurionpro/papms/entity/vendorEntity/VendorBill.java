package com.aurionpro.papms.entity.vendorEntity;

import com.aurionpro.papms.Enum.BillStatus;
import com.aurionpro.papms.entity.Organization;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "vendor_bills")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VendorBill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_id", nullable = false)
    private Vendor vendor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Column(name = "bill_number", nullable = false, unique = true)
    private String billNumber;

    @Column(name = "bill_date", nullable = false)
    private LocalDate billDate;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(nullable = false)
    private BigDecimal amount;

    @Builder.Default
    @Column(name = "paid_amount", nullable = false)
    private BigDecimal paidAmount = BigDecimal.ZERO;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private BillStatus status = BillStatus.PENDING;

    @Column(columnDefinition = "TEXT")
    private String description;

    // Optional: Link to vendor payment when paid
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_payment_id", unique = true)
    private VendorPayment vendorPayment;

    // Installment payment tracking
    @Builder.Default
    @OneToMany(mappedBy = "bill", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @OrderBy("installmentNumber ASC")
    private java.util.List<BillInstallment> installments = new java.util.ArrayList<>();

    @Column(name = "total_installments")
    private Integer totalInstallments;

    @Enumerated(EnumType.STRING)
    @Column(name = "installment_frequency")
    private com.aurionpro.papms.Enum.InstallmentFrequency installmentFrequency;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Helper method to calculate remaining due
    public BigDecimal getDueAmount() {
        return amount.subtract(paidAmount);
    }

    // Helper to check if fully paid
    public boolean isFullyPaid() {
        return paidAmount.compareTo(amount) >= 0;
    }
}
