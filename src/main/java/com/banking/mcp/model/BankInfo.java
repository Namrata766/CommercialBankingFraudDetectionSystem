package com.banking.mcp.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BankInfo {
    private String name;
    private String bic;      // 8 or 11 character SWIFT/BIC code
    private String country;  // ISO Country Code or Name (e.g., USA, Cayman Islands)
    private String address;  // Physical branch location
}