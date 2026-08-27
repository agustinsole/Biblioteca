package com.example.libros.controller;

import com.example.libros.model.Pais;
import com.example.libros.repository.PaisRepository;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/paises")
public class PaisController {

    private final PaisRepository repo;

    public PaisController(PaisRepository repo) {
        this.repo = repo;
    }

    @GetMapping
    public String list(Model model,
                       @RequestParam(defaultValue = "0") int page) {
        PageRequest pr = PageRequest.of(page, 10);
        Page<Pais> paisesPage = repo.findAllByOrderByNomPaisAsc(pr);
        model.addAttribute("paisesPage", paisesPage);
        model.addAttribute("currentPage", page);
        return "paises/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("pais", new Pais());
        model.addAttribute("banderas", getPaisBanderas());
        return "paises/form";
    }

    @PostMapping
    public String save(@Valid @ModelAttribute Pais pais, BindingResult br, Model model) {
        if (br.hasErrors()) {
            model.addAttribute("banderas", getPaisBanderas());
            model.addAttribute("errorMessage", "Hay errores en el formulario. Corrige los campos marcados y vuelve a intentarlo.");
            return "paises/form";
        }
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
        model.addAttribute("banderas", getPaisBanderas());
        return "paises/form";
    }

    private List<String> getPaisBanderas() {
        Path folder = Path.of("src/main/resources/static/img/paises");
        try {
            return Files.list(folder)
                    .filter(Files::isRegularFile)
                    .map(Path::getFileName)
                    .map(Path::toString)
                    .sorted(Comparator.naturalOrder())
                    .collect(Collectors.toList());
        } catch (IOException e) {
            return List.of();
        }
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
        Pais pais = p.get();
        List<com.example.libros.model.Autor> autoresPais = new ArrayList<>(pais.getAutores());
        autoresPais.sort(Comparator.comparing(
            a -> a.getApellido1() == null ? "" : a.getApellido1().trim().toLowerCase()
        ));
        List<com.example.libros.model.Premio> premiosPais = new ArrayList<>(pais.getPremios());
        premiosPais.sort(
                Comparator
                        .comparingInt((com.example.libros.model.Premio pr) -> {
                            String tipo = pr.getTipo() == null ? "" : pr.getTipo().trim().toUpperCase();
                            if ("A".equals(tipo)) {
                                return 0;
                            }
                            if ("L".equals(tipo)) {
                                return 1;
                            }
                            return 2;
                        })
                        .thenComparing(pr -> pr.getNomPremio() == null ? "" : pr.getNomPremio().trim().toLowerCase())
        );

        model.addAttribute("pais", pais);
        model.addAttribute("autoresPais", autoresPais);
        model.addAttribute("premiosPais", premiosPais);
        return "paises/view";
    }
}
