package com.example.libros.controller;

import com.example.libros.model.LibroPremio;
import com.example.libros.repository.LibroPremioRepository;
import com.example.libros.repository.LibroRepository;
import com.example.libros.repository.PremioRepository;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@Controller
@RequestMapping("/libro-premios")
public class LibroPremioController {

    private final LibroPremioRepository repo;
    private final LibroRepository libroRepository;
    private final PremioRepository premioRepository;

    public LibroPremioController(LibroPremioRepository repo, LibroRepository libroRepository, PremioRepository premioRepository) {
        this.repo = repo;
        this.libroRepository = libroRepository;
        this.premioRepository = premioRepository;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("libroPremios", repo.findAll());
        return "libro_premios/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("libroPremio", new LibroPremio());
        model.addAttribute("libros", libroRepository.findAll());
        model.addAttribute("premios", premioRepository.findAll());
        return "libro_premios/form";
    }

    @PostMapping
    public String save(@Valid @ModelAttribute LibroPremio libroPremio, BindingResult br, Model model) {
        if (br.hasErrors()) {
            model.addAttribute("libros", libroRepository.findAll());
            model.addAttribute("premios", premioRepository.findAll());
            return "libro_premios/form";
        }
        bindLibroAndPremio(libroPremio);
        if (libroPremio.getId() == null) {
            Optional<LibroPremio> last = repo.findTopByOrderByIdDesc();
            libroPremio.setId(last.map(l -> l.getId() + 1).orElse(1));
        }
        repo.save(libroPremio);
        return "redirect:/libro-premios";
    }

    private void bindLibroAndPremio(LibroPremio libroPremio) {
        if (libroPremio.getLibro() != null && libroPremio.getLibro().getId() != null) {
            libroRepository.findById(libroPremio.getLibro().getId()).ifPresent(libroPremio::setLibro);
        }
        if (libroPremio.getPremio() != null && libroPremio.getPremio().getId() != null) {
            premioRepository.findById(libroPremio.getPremio().getId()).ifPresent(libroPremio::setPremio);
        }
    }

    @GetMapping("/edit/{id}")
    public String editForm(@PathVariable Integer id, Model model) {
        Optional<LibroPremio> item = repo.findById(id);
        if (item.isEmpty()) return "redirect:/libro-premios";
        model.addAttribute("libroPremio", item.get());
        model.addAttribute("libros", libroRepository.findAll());
        model.addAttribute("premios", premioRepository.findAll());
        return "libro_premios/form";
    }

    @PostMapping("/delete/{id}")
    public String delete(@PathVariable Integer id) {
        repo.deleteById(id);
        return "redirect:/libro-premios";
    }

    @GetMapping("/view/{id}")
    public String view(@PathVariable Integer id, Model model) {
        Optional<LibroPremio> item = repo.findById(id);
        if (item.isEmpty()) return "redirect:/libro-premios";
        model.addAttribute("libroPremio", item.get());
        return "libro_premios/view";
    }
}
