// dto/VendorRequest.java
package com.aurionpro.papms.dto.vendorDto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class VendorRequest {
    @NotBlank(message = "Vendor name is required")
    private String vendorName;

    @Email(message = "Invalid email format")
    private String contactEmail;

    private String contactPhone;
    private String address;

    @NotBlank(message = "Account holder name is required")
    private String accountHolderName;

    @NotBlank(message = "Account number is required")
    @Size(min = 5, max = 50)
    private String accountNumber;

    @NotBlank(message = "Bank name is required")
    private String bankName;

    @NotBlank(message = "IFSC code is required")
    @Pattern(regexp = "^[A-Z]{4}0[A-Z0-9]{6}$", message = "Invalid IFSC code format")
    private String ifscCode;
}