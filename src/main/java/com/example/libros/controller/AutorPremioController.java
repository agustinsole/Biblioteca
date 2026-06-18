package com.example.libros.controller;

import com.example.libros.model.AutorPremio;
import com.example.libros.repository.AutorPremioRepository;
import com.example.libros.repository.AutorRepository;
import com.example.libros.repository.PremioRepository;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@Controller
@RequestMapping("/autor-premio")
public class AutorPremioController {

    private final AutorPremioRepository repo;
    private final AutorRepository autorRepository;
    private final PremioRepository premioRepository;

    public AutorPremioController(AutorPremioRepository repo, AutorRepository autorRepository, PremioRepository premioRepository) {
        this.repo = repo;
        this.autorRepository = autorRepository;
        this.premioRepository = premioRepository;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("autorPremios", repo.findAll());
        return "autor_premio/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("autorPremio", new AutorPremio());
        model.addAttribute("autores", autorRepository.findAll());
        model.addAttribute("premios", premioRepository.findAll());
        return "autor_premio/form";
    }

    @PostMapping
    public String save(@Valid @ModelAttribute AutorPremio autorPremio, BindingResult br, Model model) {
        if (br.hasErrors()) {
            model.addAttribute("autores", autorRepository.findAll());
            model.addAttribute("premios", premioRepository.findAll());
            return "autor_premio/form";
        }
        bindAutorAndPremio(autorPremio);
        if (autorPremio.getId() == null) {
            Optional<AutorPremio> last = repo.findTopByOrderByIdDesc();
            autorPremio.setId(last.map(a -> a.getId() + 1).orElse(1));
        }
        repo.save(autorPremio);
        return "redirect:/autor-premio";
    }

    private void bindAutorAndPremio(AutorPremio autorPremio) {
        if (autorPremio.getAutor() != null && autorPremio.getAutor().getId() != null) {
            autorRepository.findById(autorPremio.getAutor().getId()).ifPresent(autorPremio::setAutor);
        }
        if (autorPremio.getPremio() != null && autorPremio.getPremio().getId() != null) {
            premioRepository.findById(autorPremio.getPremio().getId()).ifPresent(autorPremio::setPremio);
        }
    }

    @GetMapping("/edit/{id}")
    public String editForm(@PathVariable Integer id, Model model) {
        Optional<AutorPremio> item = repo.findById(id);
        if (item.isEmpty()) return "redirect:/autor-premio";
        model.addAttribute("autorPremio", item.get());
        model.addAttribute("autores", autorRepository.findAll());
        model.addAttribute("premios", premioRepository.findAll());
        return "autor_premio/form";
    }

    @PostMapping("/delete/{id}")
    public String delete(@PathVariable Integer id) {
        repo.deleteById(id);
        return "redirect:/autor-premio";
    }

    @GetMapping("/view/{id}")
    public String view(@PathVariable Integer id, Model model) {
        Optional<AutorPremio> item = repo.findById(id);
        if (item.isEmpty()) return "redirect:/autor-premio";
        model.addAttribute("autorPremio", item.get());
        return "autor_premio/view";
    }
}
