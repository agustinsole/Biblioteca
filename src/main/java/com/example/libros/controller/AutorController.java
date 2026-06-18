package com.example.libros.controller;

import com.example.libros.model.Autor;
import com.example.libros.model.AutorPremio;
import com.example.libros.repository.AutorPremioRepository;
import com.example.libros.repository.AutorRepository;
import com.example.libros.repository.PaisRepository;
import com.example.libros.repository.PremioRepository;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.beans.PropertyEditorSupport;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;

@Controller
@RequestMapping("/autores")
public class AutorController {

    private final AutorRepository repo;
    private final PaisRepository paisRepository;
    private final PremioRepository premioRepository;
    private final AutorPremioRepository autorPremioRepository;

    public AutorController(AutorRepository repo, PaisRepository paisRepository, PremioRepository premioRepository, AutorPremioRepository autorPremioRepository) {
        this.repo = repo;
        this.paisRepository = paisRepository;
        this.premioRepository = premioRepository;
        this.autorPremioRepository = autorPremioRepository;
    }

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.registerCustomEditor(java.sql.Date.class, new PropertyEditorSupport() {
            @Override
            public void setAsText(String text) {
                if (text == null || text.trim().isEmpty()) {
                    setValue(null);
                } else {
                    try {
                        setValue(java.sql.Date.valueOf(text));
                    } catch (IllegalArgumentException e) {
                        setValue(null);
                    }
                }
            }

            @Override
            public String getAsText() {
                java.sql.Date d = (java.sql.Date) getValue();
                return d != null ? d.toString() : "";
            }
        });
    }

    @GetMapping
    public String list(Model model,
                       @RequestParam(defaultValue = "0") int page,
                       @RequestParam(required = false) String query,
                       @RequestParam(defaultValue = "id") String sortField,
                       @RequestParam(defaultValue = "asc") String sortDir) {
        Page<Autor> autoresPage;
        Sort.Direction direction = sortDir.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
        Sort sort;
        if ("nomAutor".equals(sortField)) {
            // ordenar por apellido1 y en caso de empate por apellido2
            sort = Sort.by(direction, "apellido1", "apellido2");
        } else {
            sort = Sort.by(direction, sortField);
        }
        PageRequest pr = PageRequest.of(page, 10, sort);
        if (query == null || query.trim().isEmpty()) {
            autoresPage = repo.findAll(pr);
        } else {
            autoresPage = repo.busqueda(query.trim(), pr);
        }
        model.addAttribute("autoresPage", autoresPage);
        model.addAttribute("currentPage", page);
        model.addAttribute("query", query);
        model.addAttribute("sortField", sortField);
        model.addAttribute("sortDir", sortDir);
        return "autores/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("autor", new Autor());
        model.addAttribute("paises", paisRepository.findAllByOrderByNomPaisAsc());
        model.addAttribute("fotos", getAutorFotos());
        model.addAttribute("premiosAutor", premioRepository.findAllByTipoOrderByNomPremioAsc("A"));
        return "autores/form";
    }

    @PostMapping
    public String save(@Valid @ModelAttribute Autor autor, BindingResult br, Model model) {
        if (br.hasErrors()) {
            model.addAttribute("paises", paisRepository.findAllByOrderByNomPaisAsc());
            model.addAttribute("fotos", getAutorFotos());
            model.addAttribute("premiosAutor", premioRepository.findAllByTipoOrderByNomPremioAsc("A"));
            model.addAttribute("errorMessage", "Hay errores en el formulario. Corrige los campos marcados y vuelve a intentarlo.");
            return "autores/form";
        }
        bindPais(autor);
        bindAutorPremios(autor);
        if (autor.getId() == null) {
            Optional<Autor> last = repo.findTopByOrderByIdDesc();
            autor.setId(last.map(a -> a.getId() + 1).orElse(1000));
        }
        assignAutorPremioIds(autor);
        try {
            repo.save(autor);
        } catch (Exception e) {
            model.addAttribute("paises", paisRepository.findAllByOrderByNomPaisAsc());
            model.addAttribute("fotos", getAutorFotos());
            model.addAttribute("premiosAutor", premioRepository.findAllByTipoOrderByNomPremioAsc("A"));
            model.addAttribute("errorMessage", "No se pudo guardar el autor. Revise los datos y pruebe de nuevo. " + e.getMessage());
            return "autores/form";
        }
        return "redirect:/autores";
    }

    private void bindPais(Autor autor) {
        if (autor.getPais() != null && autor.getPais().getId() != null) {
            paisRepository.findById(autor.getPais().getId()).ifPresent(autor::setPais);
        } else {
            autor.setPais(null);
        }
    }

    private void bindAutorPremios(Autor autor) {
        if (autor.getAutorPremios() == null) {
            return;
        }
        autor.getAutorPremios().removeIf(item -> item == null || item.getPremio() == null || item.getPremio().getId() == null);
        for (AutorPremio item : autor.getAutorPremios()) {
            item.setAutor(autor);
            premioRepository.findById(item.getPremio().getId()).ifPresent(item::setPremio);
        }
    }

    private void assignAutorPremioIds(Autor autor) {
        if (autor.getAutorPremios() == null) {
            return;
        }
        int nextId = autorPremioRepository.findTopByOrderByIdDesc().map(a -> a.getId() + 1).orElse(1);
        for (AutorPremio item : autor.getAutorPremios()) {
            if (item.getId() == null) {
                item.setId(nextId++);
            }
        }
    }

    @GetMapping("/edit/{id}")
    public String editForm(@PathVariable Integer id, Model model) {
        Optional<Autor> autor = repo.findById(id);
        if (autor.isEmpty()) return "redirect:/autores";
        model.addAttribute("autor", autor.get());
        model.addAttribute("paises", paisRepository.findAllByOrderByNomPaisAsc());
        model.addAttribute("fotos", getAutorFotos());
        model.addAttribute("premiosAutor", premioRepository.findAllByTipoOrderByNomPremioAsc("A"));
        return "autores/form";
    }

    private List<String> getAutorFotos() {
        Path folder = Path.of("src/main/resources/static/img/autores");
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
        return "redirect:/autores";
    }

    @GetMapping("/view/{id}")
    public String view(@PathVariable Integer id, Model model) {
        Optional<Autor> autor = repo.findById(id);
        if (autor.isEmpty()) return "redirect:/autores";
        model.addAttribute("autor", autor.get());
        return "autores/view";
    }
}
