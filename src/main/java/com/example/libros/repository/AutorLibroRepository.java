package com.example.libros.repository;

import com.example.libros.model.AutorLibro;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AutorLibroRepository extends JpaRepository<AutorLibro, Integer> {
    void deleteByLibro_Id(Integer libroId);
}
