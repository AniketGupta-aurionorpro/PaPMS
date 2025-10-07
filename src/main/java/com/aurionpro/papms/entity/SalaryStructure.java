package com.aurionpro.papms.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "salary_structures")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SalaryStructure {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Employee employee;

    @Column(name = "basic_salary", nullable = false, precision = 10, scale = 2)
    private BigDecimal basicSalary;

    @Column(name = "hra", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal hra = BigDecimal.ZERO;

    @Column(name = "da", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal da = BigDecimal.ZERO;

    @Column(name = "pf_contribution", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal pfContribution = BigDecimal.ZERO;

    @Column(name = "other_allowances", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal otherAllowances = BigDecimal.ZERO;

    @Column(name = "effective_from_date", nullable = false)
    private LocalDate effectiveFromDate;

    @Builder.Default
    @Column(name = "is_active")
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // Calculate total salary
    public BigDecimal getTotalSalary() {
        return basicSalary
                .add(hra)
                .add(da)
                .add(otherAllowances)
                .subtract(pfContribution);
    }
}