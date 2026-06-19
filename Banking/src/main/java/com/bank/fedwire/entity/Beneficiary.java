package com.bank.fedwire.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Entity
@Table(name = "beneficiary")
@IdClass(BeneficiaryId.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Beneficiary {

    @Id
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    @JsonBackReference("user-beneficiaries")
    private User user;

    @Column(name = "beneficiary_name", nullable = false, length = 140)
    private String beneficiaryName;

    @Column(name = "town_name", nullable = false, length = 35)
    private String townName;

    @Builder.Default
    @Column(name = "country_code", nullable = false, length = 2, updatable = false)
    private String countryCode = "US";

    @Id
    @Column(name = "account_number", nullable = false)
    private String accountNumber;

    @Id
    @Column(name = "routing_number", nullable = false)
    private String routingNumber;

    @Column(name = "created_date", nullable = false, updatable = false)
    private LocalDateTime createdDate;

    @Builder.Default
    @Column(nullable = false, length = 8)
    private String status = "active";

    @PrePersist
    private void prePersist() {
        normalizeDefaults();
        if (createdDate == null) {
            createdDate = LocalDateTime.now(ZoneOffset.UTC);
        }
    }

    @PreUpdate
    private void preUpdate() {
        normalizeDefaults();
    }

    private void normalizeDefaults() {
        if (countryCode == null || countryCode.isBlank()) {
            countryCode = "US";
        }
        if (status == null || status.isBlank()) {
            status = "active";
        }
        status = status.toLowerCase();
        if (!"active".equals(status) && !"inactive".equals(status)) {
            throw new IllegalArgumentException("Beneficiary status must be active or inactive");
        }
    }
}
