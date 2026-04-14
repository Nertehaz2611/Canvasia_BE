package com.example.canvasia.repository;

import com.example.canvasia.entity.PortfolioMedia;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PortfolioMediaRepository extends JpaRepository<PortfolioMedia, UUID> {

    Page<PortfolioMedia> findByPortfolioId(UUID portfolioId, Pageable pageable);
}
