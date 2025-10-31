package com.aurionpro.papms.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "job_execution_reports")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobExecutionReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "job_execution_id", nullable = false, unique = true)
    private Long jobExecutionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Column(name = "job_name", nullable = false)
    private String jobName;

    @Column(nullable = false)
    private String status;

    @Column(name = "total_records_read", nullable = false)
    private int totalRecordsRead;

    @Column(name = "successful_imports", nullable = false)
    private int successfulImports;

    @Column(name = "failed_imports", nullable = false)
    private int failedImports;

    @Lob
    @Column(name = "report_details", columnDefinition = "TEXT")
    private String reportDetails; // JSON string of failed records

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}