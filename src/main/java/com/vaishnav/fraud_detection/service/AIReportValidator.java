package com.vaishnav.fraud_detection.service;

import com.vaishnav.fraud_detection.model.AIFraudReport;

import java.util.List;

public class AIReportValidator {

    private static final List<String> ALLOWED_RECOMMENDATIONS =
            List.of("APPROVE", "REVIEW", "BLOCK");

    public static boolean isValid(AIFraudReport report) {
        if (report == null) {
            return false;
        }

        Integer score = report.getRiskScore();
        if (score == null || score < 1 || score > 10) {
            return false;
        }

        String recommendation = report.getRecommendation();
        if (recommendation == null || !ALLOWED_RECOMMENDATIONS.contains(recommendation)) {
            return false;
        }

        String category = report.getFraudCategory();
        if (category == null || category.isBlank() || category.length() > 50) {
            return false;
        }

        String explanation = report.getExplanation();
        if (explanation == null || explanation.length() > 2000) {
            return false;
        }

        return true;
    }
}
