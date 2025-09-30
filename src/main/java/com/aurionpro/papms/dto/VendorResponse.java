// dto/VendorResponse.java
package com.aurionpro.papms.dto;

import lombok.Data;

@Data
public class VendorResponse {
    private Long id;
    private String vendorName;
    private String contactEmail;
    private String contactPhone;
    private String address;
    private Boolean isActive;
    private Integer organizationId;
    private String accountHolderName;
    private String accountNumber;
    private String bankName;
    private String ifscCode;
}