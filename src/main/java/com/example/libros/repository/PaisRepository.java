package com.example.libros.repository;

import com.example.libros.model.Pais;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaisRepository extends JpaRepository<Pais, Integer> {
    Optional<Pais> findTopByOrderByIdDesc();
    List<Pais> findAllByOrderByNomPaisAsc();
    Page<Pais> findAllByOrderByNomPaisAsc(Pageable pageable);
}
