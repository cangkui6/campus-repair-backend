package com.graduation.repair.service.support;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParsedTicketData {

    private String category;
    private String location;
    private String faultPhenomenon;
    private String urgency;
    private String contact;
    private String timePreference;
    private Double confidence;
}
