package com.aurionpro.papms.service;

import com.aurionpro.papms.entity.PayrollPayment;
import com.aurionpro.papms.repository.PayrollPaymentRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PayrollExcelReportService {

    private final PayrollPaymentRepository payrollPaymentRepository;

    @Transactional(readOnly = true)
    public byte[] generatePayrollReport(Integer organizationId, int year, int month) throws IOException {
        List<PayrollPayment> payments = payrollPaymentRepository
                .findByPayrollBatchOrganizationIdAndPayrollBatchPayrollYearAndPayrollBatchPayrollMonth(organizationId, year, month);

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Payroll Report " + month + "-" + year);

            // Header
            String[] headers = {
                    "Employee ID", "Employee Name", "Employee Code", "Department",
                    "Basic Salary", "HRA", "DA", "Other Allowances", "PF Contribution", "Net Salary Paid"
            };
            Row headerRow = sheet.createRow(0);
            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            Font font = workbook.createFont();
            font.setBold(true);
            headerStyle.setFont(font);

            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Data
            int rowIdx = 1;
            for (PayrollPayment payment : payments) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(payment.getEmployee().getId());
                row.createCell(1).setCellValue(payment.getEmployee().getUser().getFullName());
                row.createCell(2).setCellValue(payment.getEmployee().getEmployeeCode());
                row.createCell(3).setCellValue(payment.getEmployee().getDepartment());
                row.createCell(4).setCellValue(payment.getBasicSalary().doubleValue());
                row.createCell(5).setCellValue(payment.getHra().doubleValue());
                row.createCell(6).setCellValue(payment.getDa().doubleValue());
                row.createCell(7).setCellValue(payment.getOtherAllowances().doubleValue());
                row.createCell(8).setCellValue(payment.getPfContribution().doubleValue());
                row.createCell(9).setCellValue(payment.getNetSalaryPaid().doubleValue());
            }

            // Auto-size columns
            for(int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();
        }
    }
}