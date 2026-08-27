package com.example.libros.controller;

import com.example.libros.model.Libro;
import com.example.libros.model.Serie;
import com.example.libros.repository.SerieRepository;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/series")
public class SerieController {

    private final SerieRepository repo;

    public SerieController(SerieRepository repo) {
        this.repo = repo;
    }

    @GetMapping
    public String list(Model model,
                       @RequestParam(defaultValue = "0") int page) {
        PageRequest pr = PageRequest.of(page, 10);
        Page<Serie> seriesPage = repo.findAllByOrderByDenominacionAsc(pr);
        model.addAttribute("seriesPage", seriesPage);
        model.addAttribute("currentPage", page);
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
        Serie currentSerie = serie.get();
        List<Libro> librosOrdenados = currentSerie.getLibros().stream()
            .sorted(Comparator.comparing(Libro::getIndiceSerie, Comparator.nullsLast(Integer::compareTo))
                .thenComparing(Libro::getNomLibro, Comparator.nullsLast(String::compareToIgnoreCase)))
            .toList();
        model.addAttribute("serie", currentSerie);
        model.addAttribute("librosOrdenados", librosOrdenados);
        return "series/view";
    }
}
