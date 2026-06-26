package com.bank.fedwire.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "dashboard_activity")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardActivity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "activity_id")
    private Long activityId;

    @Column(nullable = false, length = 100)
    private String activity;

    @Column(nullable = false, length = 500)
    private String description;

    @Column(name = "employee_name", nullable = false)
    private String employeeName;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @PrePersist
    void prePersist() {
        if (employeeName == null || employeeName.isBlank()) {
            employeeName = "System";
        }
        if (timestamp == null) {
            timestamp = LocalDateTime.now();
        }
    }
}
