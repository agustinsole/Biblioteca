package com.example.libros.controller;

import com.example.libros.model.Premio;
import com.example.libros.repository.PaisRepository;
import com.example.libros.repository.PremioRepository;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@Controller
@RequestMapping("/premios")
public class PremioController {

    private final PremioRepository repo;
    private final PaisRepository paisRepository;

    public PremioController(PremioRepository repo, PaisRepository paisRepository) {
        this.repo = repo;
        this.paisRepository = paisRepository;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("premios", repo.findAll());
        return "premios/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("premio", new Premio());
        model.addAttribute("paises", paisRepository.findAll());
        return "premios/form";
    }

    @PostMapping
    public String save(@Valid @ModelAttribute Premio premio, BindingResult br, Model model) {
        if (br.hasErrors()) {
            model.addAttribute("paises", paisRepository.findAll());
            return "premios/form";
        }
        bindPais(premio);
        if (premio.getId() == null) {
            Optional<Premio> last = repo.findTopByOrderByIdDesc();
            premio.setId(last.map(p -> p.getId() + 1).orElse(1));
        }
        repo.save(premio);
        return "redirect:/premios";
    }

    private void bindPais(Premio premio) {
        if (premio.getPais() != null && premio.getPais().getId() != null) {
            paisRepository.findById(premio.getPais().getId()).ifPresent(premio::setPais);
        } else {
            premio.setPais(null);
        }
    }

    @GetMapping("/edit/{id}")
    public String editForm(@PathVariable Integer id, Model model) {
        Optional<Premio> premio = repo.findById(id);
        if (premio.isEmpty()) return "redirect:/premios";
        model.addAttribute("premio", premio.get());
        model.addAttribute("paises", paisRepository.findAll());
        return "premios/form";
    }

    @PostMapping("/delete/{id}")
    public String delete(@PathVariable Integer id) {
        repo.deleteById(id);
        return "redirect:/premios";
    }

    @GetMapping("/view/{id}")
    public String view(@PathVariable Integer id, Model model) {
        Optional<Premio> premio = repo.findById(id);
        if (premio.isEmpty()) return "redirect:/premios";
        model.addAttribute("premio", premio.get());
        return "premios/view";
    }
}
