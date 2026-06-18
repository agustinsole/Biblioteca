package com.example.libros.controller;

import com.example.libros.model.Pais;
import com.example.libros.repository.PaisRepository;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@Controller
@RequestMapping("/paises")
public class PaisController {

    private final PaisRepository repo;

    public PaisController(PaisRepository repo) {
        this.repo = repo;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("paises", repo.findAll());
        return "paises/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("pais", new Pais());
        return "paises/form";
    }

    @PostMapping
    public String save(@Valid @ModelAttribute Pais pais, BindingResult br) {
        if (br.hasErrors()) return "paises/form";
        if (pais.getId() == null) {
            Optional<Pais> last = repo.findTopByOrderByIdDesc();
            pais.setId(last.map(p -> p.getId() + 1).orElse(1));
        }
        repo.save(pais);
        return "redirect:/paises";
    }

    @GetMapping("/edit/{id}")
    public String editForm(@PathVariable Integer id, Model model) {
        Optional<Pais> p = repo.findById(id);
        if (p.isEmpty()) return "redirect:/paises";
        model.addAttribute("pais", p.get());
        return "paises/form";
    }

    @PostMapping("/delete/{id}")
    public String delete(@PathVariable Integer id) {
        repo.deleteById(id);
        return "redirect:/paises";
    }

    @GetMapping("/view/{id}")
    public String view(@PathVariable Integer id, Model model) {
        Optional<Pais> p = repo.findById(id);
        if (p.isEmpty()) return "redirect:/paises";
        model.addAttribute("pais", p.get());
        return "paises/view";
    }
}
