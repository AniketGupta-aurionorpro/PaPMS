// utils/CsvEmployeeParser.java
package com.aurionpro.papms.utils;

import com.aurionpro.papms.dto.CsvEmployeeRecord;
import com.aurionpro.papms.dto.FailedEmployeeRecord;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class CsvEmployeeParser {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // Expected CSV headers
    private static final String[] EXPECTED_HEADERS = {
            "username", "password", "fullName", "email", "employeeCode",
            "dateOfJoining", "department", "jobTitle", "accountHolderName",
            "accountNumber", "bankName", "ifscCode", "basicSalary", "hra",
            "da", "pfContribution", "otherAllowances", "effectiveFromDate"
    };

    public CsvParseResult parseCsvFile(MultipartFile file) {
        List<CsvEmployeeRecord> validRecords = new ArrayList<>();
        List<FailedEmployeeRecord> failedRecords = new ArrayList<>();

        try (BufferedReader fileReader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8));
             CSVParser csvParser = new CSVParser(fileReader,
                     CSVFormat.DEFAULT.builder()
                             .setHeader(EXPECTED_HEADERS)
                             .setSkipHeaderRecord(true)
                             .setTrim(true)
                             .setIgnoreHeaderCase(true)
                             .build())) {

            validateCsvHeaders(csvParser);

            for (CSVRecord csvRecord : csvParser) {
                try {
                    CsvEmployeeRecord employeeRecord = parseCsvRecord(csvRecord);
                    validateEmployeeRecord(employeeRecord, csvRecord.getRecordNumber());
                    validRecords.add(employeeRecord);
                } catch (CsvValidationException e) {
                    failedRecords.add(new FailedEmployeeRecord(
                            csvRecord.getRecordNumber(),
                            csvRecord.toMap(),
                            e.getMessage()
                    ));
                }
            }

            return new CsvParseResult(validRecords, failedRecords);

        } catch (Exception e) {
            log.error("Error parsing CSV file", e);
            throw new RuntimeException("Failed to parse CSV file: " + e.getMessage(), e);
        }
    }

    private CsvEmployeeRecord parseCsvRecord(CSVRecord csvRecord) throws CsvValidationException {
        CsvEmployeeRecord record = new CsvEmployeeRecord();

        try {
            // User fields
            record.setUsername(getRequiredString(csvRecord, "username"));
            record.setPassword(getRequiredString(csvRecord, "password"));
            record.setFullName(getRequiredString(csvRecord, "fullName"));
            record.setEmail(getRequiredString(csvRecord, "email"));

            // Employee fields
            record.setEmployeeCode(getRequiredString(csvRecord, "employeeCode"));
            record.setDateOfJoining(parseDate(getRequiredString(csvRecord, "dateOfJoining")));
            record.setDepartment(getRequiredString(csvRecord, "department"));
            record.setJobTitle(getRequiredString(csvRecord, "jobTitle"));

            // Bank Account fields
            record.setAccountHolderName(getRequiredString(csvRecord, "accountHolderName"));
            record.setAccountNumber(getRequiredString(csvRecord, "accountNumber"));
            record.setBankName(getRequiredString(csvRecord, "bankName"));
            record.setIfscCode(getRequiredString(csvRecord, "ifscCode"));

            // Salary Structure fields
            record.setBasicSalary(parseBigDecimal(getRequiredString(csvRecord, "basicSalary")));
            record.setHra(parseBigDecimal(getOptionalString(csvRecord, "hra", "0")));
            record.setDa(parseBigDecimal(getOptionalString(csvRecord, "da", "0")));
            record.setPfContribution(parseBigDecimal(getOptionalString(csvRecord, "pfContribution", "0")));
            record.setOtherAllowances(parseBigDecimal(getOptionalString(csvRecord, "otherAllowances", "0")));
            record.setEffectiveFromDate(parseDate(getRequiredString(csvRecord, "effectiveFromDate")));

            return record;

        } catch (Exception e) {
            throw new CsvValidationException("Error parsing record: " + e.getMessage());
        }
    }

    private void validateEmployeeRecord(CsvEmployeeRecord record, long recordNumber) throws CsvValidationException {
        // Validate email format
        if (!isValidEmail(record.getEmail())) {
            throw new CsvValidationException("Invalid email format: " + record.getEmail());
        }

        // Validate IFSC code format
        if (!isValidIfscCode(record.getIfscCode())) {
            throw new CsvValidationException("Invalid IFSC code format: " + record.getIfscCode());
        }

        // Validate salary values
        if (record.getBasicSalary().compareTo(BigDecimal.ZERO) <= 0) {
            throw new CsvValidationException("Basic salary must be greater than zero");
        }

        // Validate dates
        if (record.getEffectiveFromDate().isBefore(record.getDateOfJoining())) {
            throw new CsvValidationException("Effective date cannot be before date of joining");
        }

        // Validate account number
        if (record.getAccountNumber().length() < 5 || record.getAccountNumber().length() > 50) {
            throw new CsvValidationException("Account number must be between 5 and 50 characters");
        }
    }

    private void validateCsvHeaders(CSVParser csvParser) {
        Map<String, Integer> headerMap = csvParser.getHeaderMap();
        for (String expectedHeader : EXPECTED_HEADERS) {
            if (!headerMap.containsKey(expectedHeader)) {
                throw new RuntimeException("Missing required column in CSV: " + expectedHeader);
            }
        }
    }

    // Helper methods
    private String getRequiredString(CSVRecord record, String column) throws CsvValidationException {
        String value = record.get(column);
        if (value == null || value.trim().isEmpty()) {
            throw new CsvValidationException("Required field '" + column + "' is missing or empty");
        }
        return value.trim();
    }

    private String getOptionalString(CSVRecord record, String column, String defaultValue) {
        String value = record.get(column);
        return (value == null || value.trim().isEmpty()) ? defaultValue : value.trim();
    }

    private LocalDate parseDate(String dateStr) throws CsvValidationException {
        try {
            return LocalDate.parse(dateStr.trim(), DATE_FORMATTER);
        } catch (DateTimeParseException e) {
            throw new CsvValidationException("Invalid date format for '" + dateStr + "'. Expected format: yyyy-MM-dd");
        }
    }

    private BigDecimal parseBigDecimal(String value) throws CsvValidationException {
        try {
            return new BigDecimal(value.trim());
        } catch (NumberFormatException e) {
            throw new CsvValidationException("Invalid number format: " + value);
        }
    }

    private boolean isValidEmail(String email) {
        return email != null && email.matches("^[A-Za-z0-9+_.-]+@(.+)$");
    }

    private boolean isValidIfscCode(String ifsc) {
        return ifsc != null && ifsc.matches("^[A-Z]{4}0[A-Z0-9]{6}$");
    }

    // Inner classes for result handling
    public static class CsvParseResult {
        private final List<CsvEmployeeRecord> validRecords;
        private final List<FailedEmployeeRecord> failedRecords;

        public CsvParseResult(List<CsvEmployeeRecord> validRecords, List<FailedEmployeeRecord> failedRecords) {
            this.validRecords = validRecords;
            this.failedRecords = failedRecords;
        }

        public List<CsvEmployeeRecord> getValidRecords() { return validRecords; }
        public List<FailedEmployeeRecord> getFailedRecords() { return failedRecords; }
    }

    public static class CsvValidationException extends Exception {
        public CsvValidationException(String message) {
            super(message);
        }
    }
}