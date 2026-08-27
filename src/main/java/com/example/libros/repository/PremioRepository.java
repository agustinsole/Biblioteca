package com.example.libros.repository;

import com.example.libros.model.Premio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface PremioRepository extends JpaRepository<Premio, Integer> {
    Optional<Premio> findTopByOrderByIdDesc();
    List<Premio> findAllByTipoOrderByNomPremioAsc(String tipo);
    Page<Premio> findByTipoOrderByNomPremioAsc(String tipo, Pageable pageable);

    @Query("select p from Premio p order by case when p.tipo = 'A' then 0 when p.tipo = 'L' then 1 else 2 end, lower(p.nomPremio) asc")
    Page<Premio> findAllOrdered(Pageable pageable);
}
