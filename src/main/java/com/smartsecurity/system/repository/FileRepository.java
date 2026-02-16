package com.smartsecurity.system.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.smartsecurity.system.entity.File;
import com.smartsecurity.system.entity.Visitor;

public interface FileRepository extends JpaRepository<File, Long> {

   Optional<File> findByVisitor(Visitor visitor);

   Optional<File> findByVisitor_Id(Long visitorId);

}
