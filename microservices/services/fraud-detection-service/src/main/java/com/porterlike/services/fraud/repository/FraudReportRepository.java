package com.porterlike.services.fraud.repository;

import com.porterlike.services.fraud.model.FraudReport;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FraudReportRepository extends JpaRepository<FraudReport, UUID> {

    List<FraudReport> findBySubjectIdOrderByCreatedAtDesc(String subjectId);

    List<FraudReport> findByReviewedFalseOrderByCreatedAtDesc();

    long countBySubjectIdAndCreatedAtAfter(String subjectId, Instant since);
}
