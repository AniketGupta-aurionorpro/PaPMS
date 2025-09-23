package com.aurionpro.papms.repository;

import com.aurionpro.papms.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Integer> {
    // We can add custom query methods here later if needed
}