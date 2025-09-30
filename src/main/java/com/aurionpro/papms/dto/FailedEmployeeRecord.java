package com.aurionpro.papms.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Map;

@Getter
@AllArgsConstructor
public class FailedEmployeeRecord {
    private long rowNumber;
    private Map<String, String> rowData;
    private String errorMessage;
}