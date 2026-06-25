package com.vaishnav.fraud_detection.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaishnav.fraud_detection.model.AIFraudReport;
import com.vaishnav.fraud_detection.model.Transaction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AIAnalysisService {

    @Value("${gemini.api.key}")
    private String apiKey;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public AIFraudReport analyze(Transaction tx,
                                 List<Transaction> recentTransactions,
                                 String triggeredRule) {

        try {

            String prompt = buildPrompt(tx, recentTransactions, triggeredRule);

            Map<String, Object> requestBody = Map.of(
                    "contents", List.of(
                            Map.of(
                                    "parts", List.of(
                                            Map.of("text", prompt)
                                    )
                            )
                    )
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> request =
                    new HttpEntity<>(requestBody, headers);

            String url =
                    "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key="
                            + apiKey;

            ResponseEntity<String> response = restTemplate.postForEntity(
                    url,
                    request,
                    String.class
            );

            JsonNode root = objectMapper.readTree(response.getBody());

            String aiResponse = root.path("candidates")
                    .get(0)
                    .path("content")
                    .path("parts")
                    .get(0)
                    .path("text")
                    .asText();

            aiResponse = aiResponse
                    .replace("```json", "")
                    .replace("```", "")
                    .trim();

            return objectMapper.readValue(aiResponse, AIFraudReport.class);

        } catch (JsonProcessingException e) {

            log.error("Failed to parse Gemini response", e);

        } catch (RestClientException e) {

            log.error("Failed to call Gemini API", e);

        } catch (Exception e) {

            log.error("Unexpected error during AI analysis", e);

        }

        return new AIFraudReport(
                5,
                "UNKNOWN",
                "AI analysis unavailable",
                "REVIEW"
        );
    }

    private String buildPrompt(Transaction tx,
                               List<Transaction> recentTransactions,
                               String triggeredRule) {

        StringBuilder history = new StringBuilder();

        for (Transaction transaction : recentTransactions) {

            history.append(String.format(
                    "- ₹%s at %s, %s on %s%n",
                    transaction.getAmount(),
                    transaction.getMerchant(),
                    transaction.getCountry(),
                    transaction.getTimestamp()
            ));
        }

        return String.format("""
                You are a senior fraud analyst working at a fintech company.

                Analyze the following transaction.

                Triggered Rule:
                %s

                Current Transaction

                Account ID: %s
                Amount: %s
                Currency: %s
                Merchant: %s
                Country: %s
                Timestamp: %s

                Recent Transactions

                %s

                Based on the transaction details, recent history, and triggered rule,
                return ONLY a valid JSON object with the following fields:

                {
                  "riskScore": 1,
                  "fraudCategory": "",
                  "explanation": "",
                  "recommendation": ""
                }

                riskScore should be between 1 and 10.
                recommendation should be APPROVE, REVIEW, or BLOCK.
                """,
                triggeredRule,
                tx.getAccountId(),
                tx.getAmount(),
                tx.getCurrency(),
                tx.getMerchant(),
                tx.getCountry(),
                tx.getTimestamp(),
                history.toString()
        );
    }
}