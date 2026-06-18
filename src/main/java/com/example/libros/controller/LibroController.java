package com.example.libros.controller;

import com.example.libros.model.Libro;
import com.example.libros.model.Serie;
import com.example.libros.repository.LibroRepository;
import com.example.libros.repository.SerieRepository;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@Controller
@RequestMapping("/libros")
public class LibroController {

    private final LibroRepository repo;
    private final SerieRepository serieRepository;

    public LibroController(LibroRepository repo, SerieRepository serieRepository) {
        this.repo = repo;
        this.serieRepository = serieRepository;
    }

    @GetMapping
    public String list(Model model,
                       @RequestParam(defaultValue = "0") int page,
                       @RequestParam(required = false) String query,
                       @RequestParam(defaultValue = "id") String sortField,
                       @RequestParam(defaultValue = "asc") String sortDir) {
        Sort.Direction direction = sortDir.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
        String safeSortField = switch (sortField) {
            case "nomLibro", "ano", "id" -> sortField;
            default -> "id";
        };
        PageRequest pr = PageRequest.of(page, 10, Sort.by(direction, safeSortField));
        Page<Libro> librosPage;
        if (query == null || query.trim().isEmpty()) {
            librosPage = repo.findAll(pr);
        } else {
            librosPage = repo.busqueda(query.trim(), pr);
        }

        model.addAttribute("librosPage", librosPage);
        model.addAttribute("currentPage", page);
        model.addAttribute("query", query);
        model.addAttribute("sortField", safeSortField);
        model.addAttribute("sortDir", sortDir);
        return "libros/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("libro", new Libro());
        model.addAttribute("series", serieRepository.findAll());
        return "libros/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute Libro libro, BindingResult br, Model model) {
        if (br.hasErrors()) {
            model.addAttribute("series", serieRepository.findAll());
            return "libros/form";
        }
        bindSerie(libro);
        if (libro.getId() == null) {
            Optional<Libro> last = repo.findTopByOrderByIdDesc();
            libro.setId(last.map(l -> l.getId() + 1).orElse(1001));
        }
        repo.save(libro);
        return "redirect:/libros";
    }

    private void bindSerie(Libro libro) {
        if (libro.getSerie() != null && libro.getSerie().getId() != null) {
            serieRepository.findById(libro.getSerie().getId()).ifPresent(libro::setSerie);
        } else {
            libro.setSerie(null);
        }
    }

    @GetMapping("/edit/{id}")
    public String editForm(@PathVariable Integer id, Model model) {
        Optional<Libro> l = repo.findById(id);
        if (l.isEmpty()) return "redirect:/libros";
        model.addAttribute("libro", l.get());
        model.addAttribute("series", serieRepository.findAll());
        return "libros/form";
    }

    @PostMapping("/delete/{id}")
    public String delete(@PathVariable Integer id) {
        repo.deleteById(id);
        return "redirect:/libros";
    }

    @GetMapping("/view/{id}")
    public String view(@PathVariable Integer id, Model model) {
        Optional<Libro> l = repo.findById(id);
        if (l.isEmpty()) return "redirect:/libros";
        model.addAttribute("libro", l.get());
        return "libros/view";
    }
}
