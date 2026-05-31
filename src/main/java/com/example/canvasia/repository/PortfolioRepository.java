package com.example.canvasia.repository;

import com.example.canvasia.entity.Portfolio;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PortfolioRepository extends JpaRepository<Portfolio, UUID> {

    List<Portfolio> findByUserIdOrderByCreatedAtAsc(UUID userId);
}
