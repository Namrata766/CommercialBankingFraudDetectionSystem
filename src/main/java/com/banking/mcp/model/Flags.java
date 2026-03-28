package com.banking.mcp.model;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Flags {
    private Boolean isSTP;
    private Boolean isSanctionHit;
    private Boolean isModified;
}