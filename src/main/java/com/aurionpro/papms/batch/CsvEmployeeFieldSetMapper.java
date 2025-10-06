package com.aurionpro.papms.batch;

import com.aurionpro.papms.dto.CsvEmployeeRecord;
import org.springframework.batch.item.file.mapping.FieldSetMapper;
import org.springframework.batch.item.file.transform.FieldSet;
import org.springframework.validation.BindException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class CsvEmployeeFieldSetMapper implements FieldSetMapper<CsvEmployeeRecord> {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Override
    public CsvEmployeeRecord mapFieldSet(FieldSet fieldSet) throws BindException {
        CsvEmployeeRecord record = new CsvEmployeeRecord();

        // User fields
        record.setUsername(fieldSet.readString("username"));
        record.setPassword(fieldSet.readString("password"));
        record.setFullName(fieldSet.readString("fullName"));
        record.setEmail(fieldSet.readString("email"));

        // Employee fields
        record.setEmployeeCode(fieldSet.readString("employeeCode"));
        record.setDateOfJoining(parseDate(fieldSet.readString("dateOfJoining")));
        record.setDepartment(fieldSet.readString("department"));
        record.setJobTitle(fieldSet.readString("jobTitle"));

        // Bank Account fields
        record.setAccountHolderName(fieldSet.readString("accountHolderName"));
        record.setAccountNumber(fieldSet.readString("accountNumber"));
        record.setBankName(fieldSet.readString("bankName"));
        record.setIfscCode(fieldSet.readString("ifscCode"));

        // Salary Structure fields
        record.setBasicSalary(parseBigDecimal(fieldSet.readString("basicSalary")));
        record.setHra(parseBigDecimal(fieldSet.readString("hra")));
        record.setDa(parseBigDecimal(fieldSet.readString("da")));
        record.setPfContribution(parseBigDecimal(fieldSet.readString("pfContribution")));
        record.setOtherAllowances(parseBigDecimal(fieldSet.readString("otherAllowances")));
        record.setEffectiveFromDate(parseDate(fieldSet.readString("effectiveFromDate")));

        return record;
    }

    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return null;
        }
        return LocalDate.parse(dateStr.trim(), DATE_FORMATTER);
    }

    private BigDecimal parseBigDecimal(String value) {
        if (value == null || value.trim().isEmpty()) {
            return BigDecimal.ZERO;
        }
        return new BigDecimal(value.trim());
    }
}