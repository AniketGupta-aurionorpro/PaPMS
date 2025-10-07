package com.aurionpro.papms.entity;

import jakarta.persistence.*;

import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

// entity/Employee.java - Add new relationships
@Entity
@Table(name = "employees", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"organization_id", "employee_code"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Employee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "user_id", referencedColumnName = "id", nullable = false, unique = true)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Organization organization;

    @Column(name = "employee_code", nullable = false)
    private String employeeCode;

    @Column(name = "date_of_joining")
    private LocalDate dateOfJoining;

    private String department;

    private String jobTitle;

    @OneToOne(mappedBy = "employee", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private BankAccount bankAccount;

    @OneToMany(mappedBy = "employee", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<SalaryStructure> salaryStructures;

    @Builder.Default
    @Column(name = "is_active")
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;


    public SalaryStructure getCurrentSalaryStructure() {
        if (salaryStructures == null) return null;
        return salaryStructures.stream()
                .filter(SalaryStructure::getIsActive)
                .findFirst()
                .orElse(null);
    }
}