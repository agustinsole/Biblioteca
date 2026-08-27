package com.example.libros.controller;

import com.example.libros.model.Autor;
import com.example.libros.model.AutorLibro;
import com.example.libros.model.Libro;
import com.example.libros.model.LibroPremio;
import com.example.libros.model.Premio;
import com.example.libros.model.Serie;
import com.example.libros.repository.AutorLibroRepository;
import com.example.libros.repository.AutorRepository;
import com.example.libros.repository.LibroRepository;
import com.example.libros.repository.LibroPremioRepository;
import com.example.libros.repository.PremioRepository;
import com.example.libros.repository.SerieRepository;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
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
@RequestMapping("/libros")
public class LibroController {

    private final LibroRepository repo;
    private final SerieRepository serieRepository;
    private final AutorRepository autorRepository;
    private final AutorLibroRepository autorLibroRepository;
    private final PremioRepository premioRepository;
    private final LibroPremioRepository libroPremioRepository;

    public LibroController(LibroRepository repo,
                           SerieRepository serieRepository,
                           AutorRepository autorRepository,
                           AutorLibroRepository autorLibroRepository,
                           PremioRepository premioRepository,
                           LibroPremioRepository libroPremioRepository) {
        this.repo = repo;
        this.serieRepository = serieRepository;
        this.autorRepository = autorRepository;
        this.autorLibroRepository = autorLibroRepository;
        this.premioRepository = premioRepository;
        this.libroPremioRepository = libroPremioRepository;
    }

    @GetMapping
    public String list(Model model,
                       @RequestParam(defaultValue = "0") int page,
                       @RequestParam(required = false) String query,
                       @RequestParam(required = false) String autorQuery,
                       @RequestParam(required = false) String premioQuery,
                       @RequestParam(defaultValue = "id") String sortField,
                       @RequestParam(defaultValue = "asc") String sortDir) {
        Sort.Direction direction = sortDir.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
        String safeSortField = switch (sortField) {
            case "nomLibro", "ano", "id" -> sortField;
            default -> "id";
        };
        PageRequest pr = PageRequest.of(page, 10, Sort.by(direction, safeSortField));
        String tituloFiltro = query == null ? null : query.trim();
        String autorFiltro = autorQuery == null ? null : autorQuery.trim();
        String premioFiltro = premioQuery == null ? null : premioQuery.trim();
        Page<Libro> librosPage = repo.busqueda(tituloFiltro, autorFiltro, premioFiltro, pr);

        model.addAttribute("librosPage", librosPage);
        model.addAttribute("currentPage", page);
        model.addAttribute("query", query);
        model.addAttribute("autorQuery", autorQuery);
        model.addAttribute("premioQuery", premioQuery);
        model.addAttribute("sortField", safeSortField);
        model.addAttribute("sortDir", sortDir);
        return "libros/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("libro", new Libro());
        model.addAttribute("series", serieRepository.findAll());
        model.addAttribute("portadas", getLibroPortadas());
        model.addAttribute("archivosPdf", getLibroArchivosPdf());
        model.addAttribute("autores", autorRepository.findAllByOrderByNomAutorAscApellido1AscApellido2Asc());
        model.addAttribute("premiosLibro", premioRepository.findAllByTipoOrderByNomPremioAsc("L"));
        return "libros/form";
    }

    @PostMapping
    @Transactional
    public String create(@Valid @ModelAttribute Libro libro, BindingResult br, Model model) {
        if (br.hasErrors()) {
            model.addAttribute("series", serieRepository.findAll());
            model.addAttribute("portadas", getLibroPortadas());
            model.addAttribute("archivosPdf", getLibroArchivosPdf());
            model.addAttribute("autores", autorRepository.findAllByOrderByNomAutorAscApellido1AscApellido2Asc());
            model.addAttribute("premiosLibro", premioRepository.findAllByTipoOrderByNomPremioAsc("L"));
            return "libros/form";
        }
        bindSerie(libro);
        bindAutorLibros(libro);
        bindLibroPremios(libro);
        if (libro.getId() == null) {
            Optional<Libro> last = repo.findTopByOrderByIdDesc();
            libro.setId(last.map(l -> l.getId() + 1).orElse(1001));
        }

        List<AutorLibro> autorItems = new ArrayList<>(libro.getAutorLibros());
        List<LibroPremio> premioItems = new ArrayList<>(libro.getLibroPremios());

        // Evita que JPA intente mergear hijos por id al guardar el libro principal.
        libro.setAutorLibros(new ArrayList<>());
        libro.setLibroPremios(new ArrayList<>());
        Libro libroPersistido = repo.save(libro);

        autorLibroRepository.deleteByLibro_Id(libro.getId());
        libroPremioRepository.deleteByLibro_Id(libro.getId());

        libro.setAutorLibros(autorItems);
        assignAutorLibroIds(libro);
        if (!libro.getAutorLibros().isEmpty()) {
            libro.getAutorLibros().forEach(item -> item.setLibro(libroPersistido));
            autorLibroRepository.saveAll(libro.getAutorLibros());
        }

        libro.setLibroPremios(premioItems);
        assignLibroPremioIds(libro);
        if (!libro.getLibroPremios().isEmpty()) {
            libro.getLibroPremios().forEach(item -> item.setLibro(libroPersistido));
            libroPremioRepository.saveAll(libro.getLibroPremios());
        }

        return "redirect:/libros";
    }

    private void bindSerie(Libro libro) {
        if (libro.getSerie() != null && libro.getSerie().getId() != null) {
            serieRepository.findById(libro.getSerie().getId()).ifPresent(libro::setSerie);
        } else {
            libro.setSerie(null);
        }
    }

    @GetMapping("/edit/{id}")
    public String editForm(@PathVariable Integer id, Model model) {
        Optional<Libro> l = repo.findById(id);
        if (l.isEmpty()) return "redirect:/libros";
        model.addAttribute("libro", l.get());
        model.addAttribute("series", serieRepository.findAll());
        model.addAttribute("portadas", getLibroPortadas());
        model.addAttribute("archivosPdf", getLibroArchivosPdf());
        model.addAttribute("autores", autorRepository.findAllByOrderByNomAutorAscApellido1AscApellido2Asc());
        model.addAttribute("premiosLibro", premioRepository.findAllByTipoOrderByNomPremioAsc("L"));
        return "libros/form";
    }

    @PostMapping("/delete/{id}")
    public String delete(@PathVariable Integer id) {
        repo.deleteById(id);
        return "redirect:/libros";
    }

    @GetMapping("/view/{id}")
    public String view(@PathVariable Integer id, Model model) {
        Optional<Libro> l = repo.findById(id);
        if (l.isEmpty()) return "redirect:/libros";
        model.addAttribute("libro", l.get());
        return "libros/view";
    }

    private List<String> getLibroPortadas() {
        Path folder = Path.of("src/main/resources/static/img/libros");
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

    private List<String> getLibroArchivosPdf() {
        Path folder = Path.of("src/main/resources/static/pdfs");
        try {
            return Files.list(folder)
                    .filter(Files::isRegularFile)
                    .map(Path::getFileName)
                    .map(Path::toString)
                    .filter(nombre -> nombre.toLowerCase().endsWith(".pdf"))
                    .sorted(Comparator.naturalOrder())
                    .collect(Collectors.toList());
        } catch (IOException e) {
            return List.of();
        }
    }

    private void bindAutorLibros(Libro libro) {
        if (libro.getAutorLibros() == null) {
            return;
        }
        libro.getAutorLibros().removeIf(item -> item == null || item.getAutor() == null || item.getAutor().getId() == null);
        for (AutorLibro item : libro.getAutorLibros()) {
            item.setId(null);
            item.setLibro(libro);
            autorRepository.findById(item.getAutor().getId()).ifPresent(item::setAutor);
        }
    }

    private void assignAutorLibroIds(Libro libro) {
        if (libro.getAutorLibros() == null) {
            return;
        }
        int nextId = autorLibroRepository.findTopByOrderByIdDesc().map(a -> a.getId() + 1).orElse(1);
        for (AutorLibro item : libro.getAutorLibros()) {
            if (item.getId() == null) {
                item.setId(nextId++);
            }
        }
    }

    private void bindLibroPremios(Libro libro) {
        if (libro.getLibroPremios() == null) {
            return;
        }
        libro.getLibroPremios().removeIf(item -> item == null || item.getPremio() == null || item.getPremio().getId() == null);
        for (LibroPremio item : libro.getLibroPremios()) {
            item.setId(null);
            item.setLibro(libro);
            premioRepository.findById(item.getPremio().getId()).ifPresent(item::setPremio);
        }
    }

    private void assignLibroPremioIds(Libro libro) {
        if (libro.getLibroPremios() == null) {
            return;
        }
        int nextId = libroPremioRepository.findTopByOrderByIdDesc().map(LibroPremio::getId).map(id -> id + 1).orElse(1);
        for (LibroPremio item : libro.getLibroPremios()) {
            if (item.getId() == null) {
                item.setId(nextId++);
            }
        }
    }
}
