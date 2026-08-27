package com.example.libros.repository;

import com.example.libros.model.Serie;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SerieRepository extends JpaRepository<Serie, Integer> {
    Optional<Serie> findTopByOrderByIdDesc();
    Page<Serie> findAllByOrderByDenominacionAsc(Pageable pageable);
}
