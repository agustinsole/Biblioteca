package com.example.libros.controller;

import com.example.libros.repository.AutorRepository;
import com.example.libros.repository.LibroRepository;
import com.example.libros.repository.PremioRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class IndexController {

    private final AutorRepository autorRepository;
    private final LibroRepository libroRepository;
    private final PremioRepository premioRepository;

    public IndexController(AutorRepository autorRepository, LibroRepository libroRepository, PremioRepository premioRepository) {
        this.autorRepository = autorRepository;
        this.libroRepository = libroRepository;
        this.premioRepository = premioRepository;
    }

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("autoresCount", autorRepository.count());
        model.addAttribute("librosCount", libroRepository.count());
        model.addAttribute("premiosCount", premioRepository.count());

        List<String> autorFotos = autorRepository.findAll().stream()
                .map(a -> a.getFoto() == null ? "" : a.getFoto().trim())
                .filter(f -> !f.isEmpty() && !"0".equals(f))
                .distinct()
                .limit(12)
                .toList();

        List<String> libroPortadas = libroRepository.findTop18ByOrderByIdDesc().stream()
                .map(l -> l.getPortada() == null ? "" : l.getPortada().trim())
                .filter(p -> !p.isEmpty() && !"0".equals(p))
                .limit(18)
                .toList();

        model.addAttribute("autorFotos", autorFotos);
        model.addAttribute("libroPortadas", libroPortadas);
        return "index";
    }
}
