package com.vaishnav.fraud_detection.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "fraud_logs")
public class FraudLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "transaction_id")
    private Transaction transaction;

    private Integer riskScore;

    private String fraudCategory;

    @Column(columnDefinition = "TEXT")
    private String explanation;

    private String recommendation;

    private String triggeredRule;

    private LocalDateTime createdAt;

    public void error(String s, Exception e) {
    }
}
