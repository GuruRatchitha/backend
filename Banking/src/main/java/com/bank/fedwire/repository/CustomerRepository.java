package com.bank.fedwire.repository;

import com.bank.fedwire.entity.Customer;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByAadharNumber(String aadharNumber);

    boolean existsByPanCardNumberIgnoreCase(String panCardNumber);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);

    boolean existsByAadharNumberAndIdNot(String aadharNumber, Long id);

    boolean existsByPanCardNumberIgnoreCaseAndIdNot(String panCardNumber, Long id);

    @Override
    @EntityGraph(attributePaths = "accounts")
    List<Customer> findAll();

    @Override
    @EntityGraph(attributePaths = "accounts")
    Optional<Customer> findById(Long id);
}
