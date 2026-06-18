package com.example.libros.repository;

import com.example.libros.model.AutorLibro;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AutorLibroRepository extends JpaRepository<AutorLibro, Integer> {
    Optional<AutorLibro> findTopByOrderByIdDesc();
}
