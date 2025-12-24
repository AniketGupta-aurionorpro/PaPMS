package com.aurionpro.papms.controller;

import com.aurionpro.papms.dto.EmployeeDashboardDto;
import com.aurionpro.papms.dto.payroll.MyPayslipHistoryDto;
import com.aurionpro.papms.dto.payroll.PayrollPaymentResponse;
import com.aurionpro.papms.service.DashboardService;
import com.aurionpro.papms.service.PayrollService;
import com.aurionpro.papms.service.PayslipPdfService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/employees/dashboard")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Employee Dashboard", description = "APIs for an employee to view their personal dashboard")
public class EmployeeDashboardController {

    private final DashboardService dashboardService;
    private final PayrollService payrollService;
    private final PayslipPdfService payslipPdfService;

    @GetMapping("/me")
    @PreAuthorize("hasRole('EMPLOYEE')")
    @Operation(summary = "Get the dashboard for the logged-in employee", description = "Retrieves a summary for the current employee, including their full profile and a summary of their latest payslip.")
    public ResponseEntity<EmployeeDashboardDto> getMyDashboard() {
        EmployeeDashboardDto employeeDashboard = dashboardService.getEmployeeDashboardData();
        return ResponseEntity.ok(employeeDashboard);
    }

    @GetMapping("/me/payslips")
    @PreAuthorize("hasRole('EMPLOYEE')")
    @Operation(summary = "Get my paginated payslip history", description = "Returns a paginated list of all historical payslips for the logged-in employee.")
    public ResponseEntity<Page<MyPayslipHistoryDto>> getMyPayslipHistory(@ParameterObject Pageable pageable) {
        return ResponseEntity.ok(payrollService.getMyPayslipHistory(pageable));
    }

    @GetMapping("/me/payslips/{paymentId}")
    @PreAuthorize("hasRole('EMPLOYEE')")
    @Operation(summary = "Get payslip details", description = "Returns the details of a specific payslip for the logged-in employee.")
    public ResponseEntity<PayrollPaymentResponse> getPayslipDetails(@PathVariable Long paymentId) {
        log.info("Request to get payslip details for payment ID {}", paymentId);
        PayrollPaymentResponse payslipData = payrollService.getPayrollPaymentDetails(paymentId);
        return ResponseEntity.ok(payslipData);
    }

    @GetMapping("/me/payslips/{paymentId}/download")
    @PreAuthorize("hasRole('EMPLOYEE')")
    @Operation(summary = "Download a salary slip as PDF", description = "Employees can download their own salary slips.")
    public ResponseEntity<byte[]> downloadPayslip(@PathVariable Long paymentId) {
        log.info("Request to download payslip PDF for payment ID {}", paymentId);
        byte[] pdfBytes = payslipPdfService.generatePayslip(paymentId);
        log.info("Successfully generated payslip PDF for payment ID {}", paymentId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "payslip-" + paymentId + ".pdf");

        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }
}