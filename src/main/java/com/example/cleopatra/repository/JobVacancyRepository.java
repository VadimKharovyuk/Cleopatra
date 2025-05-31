package com.example.cleopatra.repository;

import com.example.cleopatra.model.JobVacancy;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JobVacancyRepository extends JpaRepository<JobVacancy, Long> {
    Slice<JobVacancy> findAllBy(Pageable pageable);

}
