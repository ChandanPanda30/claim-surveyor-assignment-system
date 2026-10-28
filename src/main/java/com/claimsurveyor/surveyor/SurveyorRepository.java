package com.claimsurveyor.surveyor;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SurveyorRepository extends JpaRepository<Surveyor, Long> {

    List<Surveyor> findByActiveTrueOrderByNameAsc();
}
