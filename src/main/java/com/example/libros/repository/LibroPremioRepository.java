package com.example.libros.repository;

import com.example.libros.model.LibroPremio;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LibroPremioRepository extends JpaRepository<LibroPremio, Integer> {
    Optional<LibroPremio> findTopByOrderByIdDesc();

    void deleteByLibro_Id(Integer libroId);
}
