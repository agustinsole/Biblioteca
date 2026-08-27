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
        java.util.List<Autor> findAllByOrderByNomAutorAscApellido1AscApellido2Asc();

    @Query(value = "SELECT DISTINCT a FROM Autor a " +
            "LEFT JOIN a.autorPremios ap " +
            "LEFT JOIN ap.premio p " +
            "WHERE (:query IS NULL OR :query = '' OR " +
            "LOWER(a.nomAutor) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(a.apellido1) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(a.apellido2) LIKE LOWER(CONCAT('%', :query, '%'))) " +
            "AND (:premioQuery IS NULL OR :premioQuery = '' OR " +
            "LOWER(COALESCE(p.nomPremio, '')) LIKE LOWER(CONCAT('%', :premioQuery, '%')))",
            countQuery = "SELECT COUNT(DISTINCT a.id) FROM Autor a " +
                    "LEFT JOIN a.autorPremios ap " +
                    "LEFT JOIN ap.premio p " +
                    "WHERE (:query IS NULL OR :query = '' OR " +
                    "LOWER(a.nomAutor) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
                    "LOWER(a.apellido1) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
                    "LOWER(a.apellido2) LIKE LOWER(CONCAT('%', :query, '%'))) " +
                    "AND (:premioQuery IS NULL OR :premioQuery = '' OR " +
                    "LOWER(COALESCE(p.nomPremio, '')) LIKE LOWER(CONCAT('%', :premioQuery, '%')))")
    Page<Autor> busqueda(@Param("query") String query,
                         @Param("premioQuery") String premioQuery,
                         Pageable pageable);
}
