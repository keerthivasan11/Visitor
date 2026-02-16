package com.smartsecurity.system.repository;


import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.stereotype.Repository;


import com.smartsecurity.system.entity.Staff;
import com.smartsecurity.system.enums.VisitStatus;


@Repository
public interface StaffRepository extends JpaRepository<Staff, Integer> {

    Optional<Staff> findByMobileNumber(String mobileNumber);

    Object countByStatus(VisitStatus checkedIn);
    
}
