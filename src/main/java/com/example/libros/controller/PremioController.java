package com.example.libros.controller;

import com.example.libros.model.Premio;
import com.example.libros.model.AutorPremio;
import com.example.libros.model.LibroPremio;
import com.example.libros.repository.PaisRepository;
import com.example.libros.repository.PremioRepository;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
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
@RequestMapping("/premios")
public class PremioController {

    private final PremioRepository repo;
    private final PaisRepository paisRepository;

    public PremioController(PremioRepository repo, PaisRepository paisRepository) {
        this.repo = repo;
        this.paisRepository = paisRepository;
    }

    @GetMapping
    public String list(Model model,
                       @RequestParam(defaultValue = "0") int page,
                       @RequestParam(required = false) String tipo) {
        PageRequest pr = PageRequest.of(page, 10);
        String tipoFiltro = tipo == null ? null : tipo.trim();
        Page<Premio> premiosPage;
        if (tipoFiltro == null || tipoFiltro.isEmpty()) {
            premiosPage = repo.findAllOrdered(pr);
        } else {
            premiosPage = repo.findByTipoOrderByNomPremioAsc(tipoFiltro, pr);
        }
        model.addAttribute("premiosPage", premiosPage);
        model.addAttribute("currentPage", page);
        model.addAttribute("tipo", tipoFiltro == null ? "" : tipoFiltro);
        return "premios/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("premio", new Premio());
        model.addAttribute("paises", paisRepository.findAll());
        model.addAttribute("logotipos", getPremioLogotipos());
        return "premios/form";
    }

    @PostMapping
    public String save(@Valid @ModelAttribute Premio premio, BindingResult br, Model model) {
        if (br.hasErrors()) {
            model.addAttribute("paises", paisRepository.findAll());
            model.addAttribute("logotipos", getPremioLogotipos());
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
        model.addAttribute("logotipos", getPremioLogotipos());
        return "premios/form";
    }

    private List<String> getPremioLogotipos() {
        Path folder = Path.of("src/main/resources/static/img/premios");
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
        return "redirect:/premios";
    }

    @GetMapping("/view/{id}")
    public String view(@PathVariable Integer id,
                       @RequestParam(defaultValue = "0") int page,
                       Model model) {
        Optional<Premio> premio = repo.findById(id);
        if (premio.isEmpty()) return "redirect:/premios";

        Premio currentPremio = premio.get();
        int pageSize = 10;
        PageRequest pr = PageRequest.of(page, pageSize);

        if ("A".equalsIgnoreCase(currentPremio.getTipo())) {
            List<AutorPremio> sortedAutorPremios = currentPremio.getAutorPremios().stream()
                    .filter(ap -> ap != null && ap.getAutor() != null)
                    .sorted(Comparator
                            .comparing((AutorPremio ap) -> ap.getAnio() == null ? Integer.MAX_VALUE : ap.getAnio())
                            .thenComparing(ap -> ap.getAutor().getApellido1() == null ? "" : ap.getAutor().getApellido1(), String.CASE_INSENSITIVE_ORDER)
                            .thenComparing(ap -> ap.getAutor().getApellido2() == null ? "" : ap.getAutor().getApellido2(), String.CASE_INSENSITIVE_ORDER)
                            .thenComparing(ap -> ap.getAutor().getNomAutor() == null ? "" : ap.getAutor().getNomAutor(), String.CASE_INSENSITIVE_ORDER))
                    .collect(Collectors.toCollection(ArrayList::new));

            int start = Math.min(page * pageSize, sortedAutorPremios.size());
            int end = Math.min(start + pageSize, sortedAutorPremios.size());
            Page<AutorPremio> autorPremiosPage = new PageImpl<>(sortedAutorPremios.subList(start, end), pr, sortedAutorPremios.size());
            model.addAttribute("autorPremiosPage", autorPremiosPage);
        } else if ("L".equalsIgnoreCase(currentPremio.getTipo())) {
            List<LibroPremio> sortedLibroPremios = currentPremio.getLibroPremios().stream()
                    .filter(lp -> lp != null && lp.getLibro() != null)
                    .sorted(Comparator
                            .comparing((LibroPremio lp) -> lp.getLibro().getAno() == null ? Integer.MAX_VALUE : lp.getLibro().getAno())
                            .thenComparing(lp -> lp.getLibro().getNomLibro() == null ? "" : lp.getLibro().getNomLibro(), String.CASE_INSENSITIVE_ORDER))
                    .collect(Collectors.toCollection(ArrayList::new));

            int start = Math.min(page * pageSize, sortedLibroPremios.size());
            int end = Math.min(start + pageSize, sortedLibroPremios.size());
            Page<LibroPremio> libroPremiosPage = new PageImpl<>(sortedLibroPremios.subList(start, end), pr, sortedLibroPremios.size());
            model.addAttribute("libroPremiosPage", libroPremiosPage);
        }

        model.addAttribute("currentPage", page);
        model.addAttribute("premio", currentPremio);
        return "premios/view";
    }
}
