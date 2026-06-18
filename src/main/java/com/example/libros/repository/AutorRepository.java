package com.example.libros.repository;

import com.example.libros.model.Autor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface AutorRepository extends JpaRepository<Autor, Integer> {
    Optional<Autor> findTopByOrderByIdDesc();

    @Query(value = "SELECT a FROM Autor a WHERE LOWER(a.nomAutor) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(a.apellido1) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(a.apellido2) LIKE LOWER(CONCAT('%', :query, '%'))",
           countQuery = "SELECT COUNT(a) FROM Autor a WHERE LOWER(a.nomAutor) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(a.apellido1) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(a.apellido2) LIKE LOWER(CONCAT('%', :query, '%'))")
    Page<Autor> busqueda(@Param("query") String query, Pageable pageable);
}
