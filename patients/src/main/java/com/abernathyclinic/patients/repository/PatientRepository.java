package com.abernathyclinic.patients.repository;

import com.abernathyclinic.patients.model.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface PatientRepository extends JpaRepository<Patient, Integer> {
    Optional<Patient> findByFamilyAndGivenAndDob(String family, String given, LocalDate dob);

    List<Patient> findByFamily(String family);
}
