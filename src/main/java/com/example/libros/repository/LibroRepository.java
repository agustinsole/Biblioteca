package com.example.libros.repository;

import com.example.libros.model.Libro;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface LibroRepository extends JpaRepository<Libro, Integer> {
    Optional<Libro> findTopByOrderByIdDesc();

    @Query("""
            select distinct l
            from Libro l
            left join l.serie s
            where lower(l.nomLibro) like lower(concat('%', :query, '%'))
               or lower(coalesce(s.denominacion, '')) like lower(concat('%', :query, '%'))
               or lower(coalesce(l.nomArchivo, '')) like lower(concat('%', :query, '%'))
            """)
    Page<Libro> busqueda(@Param("query") String query, Pageable pageable);
}
