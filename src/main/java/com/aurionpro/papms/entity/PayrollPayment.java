package com.aurionpro.papms.entity;

import com.aurionpro.papms.Enum.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payroll_payments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PayrollPayment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payroll_batch_id", nullable = false)
    private PayrollBatch payrollBatch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(name = "net_salary_paid", nullable = false, precision = 10, scale = 2)
    private BigDecimal netSalaryPaid;

    // Snapshot of salary structure at the time of payment
    @Column(name = "basic_salary", precision = 10, scale = 2)
    private BigDecimal basicSalary;
    @Column(name = "hra", precision = 10, scale = 2)
    private BigDecimal hra;
    @Column(name = "da", precision = 10, scale = 2)
    private BigDecimal da;
    @Column(name = "pf_contribution", precision = 10, scale = 2)
    private BigDecimal pfContribution;
    @Column(name = "other_allowances", precision = 10, scale = 2)
    private BigDecimal otherAllowances;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}