package com.example.libros.repository;

import com.example.libros.model.Premio;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PremioRepository extends JpaRepository<Premio, Integer> {
    Optional<Premio> findTopByOrderByIdDesc();
    List<Premio> findAllByTipoOrderByNomPremioAsc(String tipo);
}
