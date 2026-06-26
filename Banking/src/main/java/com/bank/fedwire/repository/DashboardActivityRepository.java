package com.bank.fedwire.repository;

import com.bank.fedwire.dto.RecentActivityResponse;
import com.bank.fedwire.entity.DashboardActivity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DashboardActivityRepository extends JpaRepository<DashboardActivity, Long> {

    @Query("""
            select new com.bank.fedwire.dto.RecentActivityResponse(
                    a.activity,
                    a.description,
                    a.employeeName,
                    a.timestamp
            )
            from DashboardActivity a
            order by a.timestamp desc
            """)
    List<RecentActivityResponse> findRecentActivities(Pageable pageable);
}
