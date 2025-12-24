package com.aurionpro.papms.service;

import com.aurionpro.papms.Enum.Role;
import com.aurionpro.papms.entity.Transaction;
import com.aurionpro.papms.entity.User;
import com.aurionpro.papms.repository.AppUserRepository;
import com.aurionpro.papms.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionExcelReportService {

    private final TransactionRepository transactionRepository;
    private final AppUserRepository userRepository;

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Transactional(readOnly = true)
    public byte[] generateTransactionReport(Integer organizationId) throws IOException {
        User currentUser = getLoggedInUser();

        // Security Check
        if (currentUser.getRole() == Role.ORG_ADMIN && !currentUser.getOrganizationId().equals(organizationId)) {
            throw new SecurityException("You are not authorized to generate reports for this organization.");
        }

        log.info("Generating transaction Excel report for organization ID: {}", organizationId);
        List<Transaction> transactions = transactionRepository
                .findAllByOrganizationIdOrderByTransactionDateDesc(organizationId);

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Transactions");

            // --- Header ---
            String[] headers = {
                    "Transaction ID", "Date", "Type", "Description",
                    "Source Type", "Source ID", "Amount", "Balance After"
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

            // --- Data ---
            int rowIdx = 1;
            for (Transaction tx : transactions) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(tx.getId());
                row.createCell(1).setCellValue(tx.getTransactionDate().format(DATE_TIME_FORMATTER));
                row.createCell(2).setCellValue(tx.getTransactionType().name());
                row.createCell(3).setCellValue(tx.getDescription());
                row.createCell(4).setCellValue(tx.getSourceType().name());
                row.createCell(5).setCellValue(tx.getSourceId());
                row.createCell(6).setCellValue(tx.getAmount().doubleValue());
                row.createCell(7).setCellValue(tx.getBalanceAfterTransaction().doubleValue());
            }

            // Auto-size columns for readability
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            log.info("Successfully generated Excel report with {} transactions.", transactions.size());
            return out.toByteArray();
        }
    }

    private User getLoggedInUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found."));
    }
}