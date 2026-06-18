package com.example.libros.controller;

import com.example.libros.model.Serie;
import com.example.libros.repository.SerieRepository;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@Controller
@RequestMapping("/series")
public class SerieController {

    private final SerieRepository repo;

    public SerieController(SerieRepository repo) {
        this.repo = repo;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("series", repo.findAll());
        return "series/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("serie", new Serie());
        return "series/form";
    }

    @PostMapping
    public String save(@Valid @ModelAttribute Serie serie, BindingResult br) {
        if (br.hasErrors()) return "series/form";
        if (serie.getId() == null) {
            Optional<Serie> last = repo.findTopByOrderByIdDesc();
            serie.setId(last.map(s -> s.getId() + 1).orElse(1));
        }
        repo.save(serie);
        return "redirect:/series";
    }

    @GetMapping("/edit/{id}")
    public String editForm(@PathVariable Integer id, Model model) {
        Optional<Serie> serie = repo.findById(id);
        if (serie.isEmpty()) return "redirect:/series";
        model.addAttribute("serie", serie.get());
        return "series/form";
    }

    @PostMapping("/delete/{id}")
    public String delete(@PathVariable Integer id) {
        repo.deleteById(id);
        return "redirect:/series";
    }

    @GetMapping("/view/{id}")
    public String view(@PathVariable Integer id, Model model) {
        Optional<Serie> serie = repo.findById(id);
        if (serie.isEmpty()) return "redirect:/series";
        model.addAttribute("serie", serie.get());
        return "series/view";
    }
}
