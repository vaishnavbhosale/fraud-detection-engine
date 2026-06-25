package com.vaishnav.fraud_detection.service;

import com.vaishnav.fraud_detection.model.FraudLog;
import com.vaishnav.fraud_detection.model.Transaction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlertService {

    private final JavaMailSender mailSender;

    @Value("${alert.email}")
    private String alertEmail;

    public void sendFraudAlert(Transaction tx, FraudLog log) {

        try {

            SimpleMailMessage message = new SimpleMailMessage();

            message.setTo(alertEmail);

            message.setSubject("🚨 Fraud Alert - Transaction #" + tx.getId());

            message.setText("""
                    Fraudulent transaction detected.

                    Transaction Id : %d
                    Account Id     : %s
                    Amount         : %s
                    Merchant       : %s
                    Country        : %s

                    AI Analysis

                    Risk Score     : %d
                    Category       : %s
                    Recommendation : %s

                    Explanation:
                    %s
                    """.formatted(
                    tx.getId(),
                    tx.getAccountId(),
                    tx.getAmount(),
                    tx.getMerchant(),
                    tx.getCountry(),
                    log.getRiskScore(),
                    log.getFraudCategory(),
                    log.getRecommendation(),
                    log.getExplanation()
            ));

            mailSender.send(message);

        } catch (Exception e) {
            log.error("Failed to send fraud alert email", e);
        }
    }
}