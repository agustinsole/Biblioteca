package com.example.libros.repository;

import com.example.libros.model.AutorPremio;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AutorPremioRepository extends JpaRepository<AutorPremio, Integer> {
    Optional<AutorPremio> findTopByOrderByIdDesc();
}
