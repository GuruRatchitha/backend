package com.bank.fedwire.repository;

import com.bank.fedwire.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    @EntityGraph(attributePaths = "role")
    Optional<User> findByEmailIgnoreCase(String email);

    @EntityGraph(attributePaths = {"role", "accounts"})
    Optional<User> findWithRoleAndAccountsByUserId(Long userId);

    @EntityGraph(attributePaths = "role")
    Optional<User> findWithRoleByUserId(Long userId);

    @EntityGraph(attributePaths = {"role", "accounts"})
    List<User> findByRoleRoleId(Long roleId);

    Page<User> findByRoleRoleId(Long roleId, Pageable pageable);

    long countByRoleRoleNameIgnoreCase(String roleName);

    long countByRoleRoleNameIgnoreCaseAndCreatedDateBetween(
            String roleName,
            LocalDateTime startDate,
            LocalDateTime endDate);

    @Query("""
            select new com.bank.fedwire.dto.RecentCustomerResponse(
                    u.userId,
                    u.userName,
                    u.email,
                    u.phoneNumber,
                    u.createdDate
            )
            from User u
            where upper(u.role.roleName) = upper(:roleName)
            order by u.createdDate desc
            """)
    List<com.bank.fedwire.dto.RecentCustomerResponse> findRecentCustomers(
            @Param("roleName") String roleName,
            Pageable pageable);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByAadharNumber(String aadharNumber);

    boolean existsByPanCardNumberIgnoreCase(String panCardNumber);

    boolean existsByEmailIgnoreCaseAndUserIdNot(String email, Long userId);

    boolean existsByAadharNumberAndUserIdNot(String aadharNumber, Long userId);

    boolean existsByPanCardNumberIgnoreCaseAndUserIdNot(String panCardNumber, Long userId);
}
