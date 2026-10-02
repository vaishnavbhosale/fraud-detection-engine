package com.vaishnav.fraud_detection.rules;
import com.vaishnav.fraud_detection.model.AIFraudReport;
import com.vaishnav.fraud_detection.service.AIReportValidator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AIReportValidatorTest {

    private AIFraudReport validReport() {
        return new AIFraudReport(7, "ACCOUNT_TAKEOVER", "Looks risky", "BLOCK");
    }

    @Test
    void shouldAcceptAValidReport() {
        assertTrue(AIReportValidator.isValid(validReport()));
    }

    @Test
    void shouldRejectNullReport() {
        assertFalse(AIReportValidator.isValid(null));
    }

    @Test
    void shouldRejectMissingScore() {
        AIFraudReport report = validReport();
        report.setRiskScore(null);

        assertFalse(AIReportValidator.isValid(report));
    }

    @Test
    void shouldRejectScoreOutsideOneToTen() {
        AIFraudReport tooLow = validReport();
        tooLow.setRiskScore(0);

        AIFraudReport tooHigh = validReport();
        tooHigh.setRiskScore(11);

        assertFalse(AIReportValidator.isValid(tooLow));
        assertFalse(AIReportValidator.isValid(tooHigh));
    }

    @Test
    void shouldRejectUnknownRecommendation() {
        AIFraudReport report = validReport();
        report.setRecommendation("DELETE_EVERYTHING");

        assertFalse(AIReportValidator.isValid(report));
    }

    @Test
    void shouldRejectMissingRecommendation() {
        AIFraudReport report = validReport();
        report.setRecommendation(null);

        assertFalse(AIReportValidator.isValid(report));
    }

    @Test
    void shouldRejectVeryLongCategory() {
        AIFraudReport report = validReport();
        report.setFraudCategory("x".repeat(51));

        assertFalse(AIReportValidator.isValid(report));
    }

    @Test
    void shouldRejectMissingExplanation() {
        AIFraudReport report = validReport();
        report.setExplanation(null);

        assertFalse(AIReportValidator.isValid(report));
    }
}
