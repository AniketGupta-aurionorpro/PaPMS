package com.aurionpro.papms.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FailedEmployeeRecord {
    private long rowNumber;
    private Map<String, String> rowData;
    private String errorMessage;
}