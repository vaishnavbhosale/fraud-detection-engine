package com.vaishnav.fraud_detection.model;

import jakarta.persistence.Entity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AIFraudReport {

    private Integer riskScore;

    private String fraudCategory;

    private String explanation;

    private String recommendation;



}
