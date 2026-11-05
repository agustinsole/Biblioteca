package com.example.libros.repository;

import com.example.libros.model.Libro;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface LibroRepository extends JpaRepository<Libro, Integer> {
    List<Libro> findTop18ByOrderByIdDesc();

    @Query(
            value = "select distinct l " +
                    "from Libro l " +
                    "left join l.autorLibros al " +
                    "left join al.autor a " +
                    "left join l.libroPremios lp " +
                    "left join lp.premio p " +
                    "where (:titulo is null or :titulo = '' or lower(l.nomLibro) like lower(concat('%', :titulo, '%'))) " +
                    "and (:autor is null or :autor = '' " +
                    "or lower(trim(concat(concat(concat(coalesce(a.nomAutor, ''), ' '), concat(coalesce(a.apellido1, ''), ' ')), coalesce(a.apellido2, '')))) like lower(concat('%', :autor, '%'))) " +
                    "and (:premio is null or :premio = '' or lower(coalesce(p.nomPremio, '')) like lower(concat('%', :premio, '%')))",
            countQuery = "select count(distinct l.id) " +
                    "from Libro l " +
                    "left join l.autorLibros al " +
                    "left join al.autor a " +
                    "left join l.libroPremios lp " +
                    "left join lp.premio p " +
                    "where (:titulo is null or :titulo = '' or lower(l.nomLibro) like lower(concat('%', :titulo, '%'))) " +
                    "and (:autor is null or :autor = '' " +
                    "or lower(trim(concat(concat(concat(coalesce(a.nomAutor, ''), ' '), concat(coalesce(a.apellido1, ''), ' ')), coalesce(a.apellido2, '')))) like lower(concat('%', :autor, '%'))) " +
                    "and (:premio is null or :premio = '' or lower(coalesce(p.nomPremio, '')) like lower(concat('%', :premio, '%')))"
    )
    Page<Libro> busqueda(@Param("titulo") String titulo,
                    @Param("autor") String autor,
                    @Param("premio") String premio,
                    Pageable pageable);
}
