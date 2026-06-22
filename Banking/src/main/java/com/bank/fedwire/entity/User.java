package com.bank.fedwire.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userId;

    private String userName;

    private String password;

    private String email;

    private String aadharNumber;

    private String panCardNumber;

    private String phoneNumber;

    private String address;

    @Builder.Default
    private String countryCode = "us";

    private String townName;

    private LocalDateTime createdDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "roleId")
    @JsonBackReference("role-users")
    private Role role;

    @OneToMany(mappedBy = "user")
    @JsonManagedReference("user-accounts")
    @Builder.Default
    private List<Account> accounts = new ArrayList<>();

    @OneToMany(mappedBy = "user")
    @JsonManagedReference("user-beneficiaries")
    @Builder.Default
    private List<Beneficiary> beneficiaries = new ArrayList<>();
}
