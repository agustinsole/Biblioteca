package com.example.libros.repository;

import com.example.libros.model.LibroPremio;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LibroPremioRepository extends JpaRepository<LibroPremio, Integer> {
    void deleteByLibro_Id(Integer libroId);
}
