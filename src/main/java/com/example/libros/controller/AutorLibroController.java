package com.example.libros.controller;

import com.example.libros.model.AutorLibro;
import com.example.libros.repository.AutorLibroRepository;
import com.example.libros.repository.AutorRepository;
import com.example.libros.repository.LibroRepository;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@Controller
@RequestMapping("/autor-libro")
public class AutorLibroController {

    private final AutorLibroRepository repo;
    private final AutorRepository autorRepository;
    private final LibroRepository libroRepository;

    public AutorLibroController(AutorLibroRepository repo, AutorRepository autorRepository, LibroRepository libroRepository) {
        this.repo = repo;
        this.autorRepository = autorRepository;
        this.libroRepository = libroRepository;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("autorLibros", repo.findAll());
        return "autor_libro/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("autorLibro", new AutorLibro());
        model.addAttribute("autores", autorRepository.findAll());
        model.addAttribute("libros", libroRepository.findAll());
        return "autor_libro/form";
    }

    @PostMapping
    public String save(@Valid @ModelAttribute AutorLibro autorLibro, BindingResult br, Model model) {
        if (br.hasErrors()) {
            model.addAttribute("autores", autorRepository.findAll());
            model.addAttribute("libros", libroRepository.findAll());
            return "autor_libro/form";
        }
        bindAutorAndLibro(autorLibro);
        repo.save(autorLibro);
        return "redirect:/autor-libro";
    }

    private void bindAutorAndLibro(AutorLibro autorLibro) {
        if (autorLibro.getAutor() != null && autorLibro.getAutor().getId() != null) {
            autorRepository.findById(autorLibro.getAutor().getId()).ifPresent(autorLibro::setAutor);
        }
        if (autorLibro.getLibro() != null && autorLibro.getLibro().getId() != null) {
            libroRepository.findById(autorLibro.getLibro().getId()).ifPresent(autorLibro::setLibro);
        }
    }

    @GetMapping("/edit/{id}")
    public String editForm(@PathVariable Integer id, Model model) {
        Optional<AutorLibro> item = repo.findById(id);
        if (item.isEmpty()) return "redirect:/autor-libro";
        model.addAttribute("autorLibro", item.get());
        model.addAttribute("autores", autorRepository.findAll());
        model.addAttribute("libros", libroRepository.findAll());
        return "autor_libro/form";
    }

    @PostMapping("/delete/{id}")
    public String delete(@PathVariable Integer id) {
        repo.deleteById(id);
        return "redirect:/autor-libro";
    }

    @GetMapping("/view/{id}")
    public String view(@PathVariable Integer id, Model model) {
        Optional<AutorLibro> item = repo.findById(id);
        if (item.isEmpty()) return "redirect:/autor-libro";
        model.addAttribute("autorLibro", item.get());
        return "autor_libro/view";
    }
}
