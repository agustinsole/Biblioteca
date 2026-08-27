package com.example.libros.controller;

import com.example.libros.model.Autor;
import com.example.libros.model.Libro;
import com.example.libros.repository.AutorRepository;
import com.example.libros.service.IAService;
import com.example.libros.service.RAGService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Controller
@RequestMapping("/ia")
public class IAController {

    private final IAService iaService;
    private final RAGService ragService;
    private final AutorRepository autorRepository;

    public IAController(IAService iaService, RAGService ragService, AutorRepository autorRepository) {
        this.iaService = iaService;
        this.ragService = ragService;
        this.autorRepository = autorRepository;
    }

    @GetMapping
    public String form(Model model) {
        model.addAttribute("pregunta", "");
        return "IA/form";
    }

    @PostMapping("/preguntar")
    public String preguntar(@RequestParam("pregunta") String pregunta, Model model) {
        String preguntaLimpia = pregunta == null ? "" : pregunta.trim();
        model.addAttribute("pregunta", preguntaLimpia);

        if (preguntaLimpia.isEmpty()) {
            model.addAttribute("error", "Escribe una pregunta antes de enviar.");
            return "IA/form";
        }

        try {
            String respuesta = iaService.generarRespuesta(preguntaLimpia);
            model.addAttribute("respuesta", respuesta);
        } catch (IllegalStateException ex) {
            model.addAttribute("error", ex.getMessage());
        }

        return "IA/form";
    }

    @GetMapping("/rag")
    public String ragForm(Model model) {
        cargarDatosRagForm(model, "", "");
        return "IA/rag-form";
    }

    @PostMapping("/rag/preguntar")
    public String preguntarRag(@RequestParam("pdfFile") String pdfFile,
                               @RequestParam("pregunta") String pregunta,
                               Model model) {
        String pdfSeleccionado = pdfFile == null ? "" : pdfFile.trim();
        String preguntaLimpia = pregunta == null ? "" : pregunta.trim();
        cargarDatosRagForm(model, pdfSeleccionado, preguntaLimpia);

        if (pdfSeleccionado.isEmpty()) {
            model.addAttribute("error", "Selecciona un libro del autor antes de preguntar.");
            return "IA/rag-form";
        }
        if (preguntaLimpia.isEmpty()) {
            model.addAttribute("error", "Escribe una pregunta sobre el PDF.");
            return "IA/rag-form";
        }

        try {
            String respuesta = ragService.responderPreguntaSobrePdf(pdfSeleccionado, preguntaLimpia);
            model.addAttribute("respuesta", respuesta);
        } catch (IllegalStateException ex) {
            model.addAttribute("error", ex.getMessage());
        }

        return "IA/rag-form";
    }

    private void cargarDatosRagForm(Model model, String pdfSeleccionado, String preguntaRag) {
        List<AutorRagOption> autoresRag = construirOpcionesAutoresRag();
        String autorSeleccionado = resolverAutorSeleccionado(autoresRag, pdfSeleccionado);
        model.addAttribute("autoresRag", autoresRag);
        model.addAttribute("autorSeleccionado", autorSeleccionado);
        model.addAttribute("pdfSeleccionado", pdfSeleccionado == null ? "" : pdfSeleccionado);
        model.addAttribute("preguntaRag", preguntaRag == null ? "" : preguntaRag);
    }

    private List<AutorRagOption> construirOpcionesAutoresRag() {
        List<Autor> autores = new ArrayList<>(autorRepository.findAll());
        autores.sort(Comparator
                .comparing((Autor a) -> normalizarParaOrden(safe(a.getApellido1())))
                .thenComparing(a -> normalizarParaOrden(safe(a.getApellido2())))
                .thenComparing(a -> normalizarParaOrden(safe(a.getNomAutor()))));

        List<AutorRagOption> resultado = new ArrayList<>();
        for (Autor autor : autores) {
            List<LibroRagOption> libros = autor.getLibrosOrdenadosPorAnoAsc().stream()
                    .filter(Objects::nonNull)
                    .filter(libro -> libro.getNomArchivo() != null && !libro.getNomArchivo().isBlank())
                    .filter(libro -> !"0".equals(libro.getNomArchivo().trim()))
                    .map(libro -> new LibroRagOption(libro.getNomLibro(), libro.getNomArchivo().trim()))
                    .distinct()
                    .toList();

            if (!libros.isEmpty()) {
                resultado.add(new AutorRagOption(
                        String.valueOf(autor.getId()),
                        construirEtiquetaAutor(autor),
                        libros
                ));
            }
        }

        return resultado;
    }

    private String resolverAutorSeleccionado(List<AutorRagOption> autores, String pdfSeleccionado) {
        if (pdfSeleccionado == null || pdfSeleccionado.isBlank()) {
            return "";
        }
        for (AutorRagOption autor : autores) {
            boolean coincide = autor.libros().stream().anyMatch(libro -> libro.pdfFile().equalsIgnoreCase(pdfSeleccionado));
            if (coincide) {
                return autor.id();
            }
        }
        return "";
    }

    private String construirEtiquetaAutor(Autor autor) {
        String ap1 = safe(autor.getApellido1());
        String ap2 = safe(autor.getApellido2());
        String nombre = safe(autor.getNomAutor());
        String apellidos = (ap1 + " " + ap2).trim();
        if (apellidos.isBlank()) {
            return nombre;
        }
        if (nombre.isBlank()) {
            return apellidos;
        }
        return apellidos + ", " + nombre;
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private String normalizarParaOrden(String texto) {
        String sinAcentos = Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        return sinAcentos.toLowerCase(Locale.ROOT);
    }

    public record AutorRagOption(String id, String label, List<LibroRagOption> libros) {
    }

    public record LibroRagOption(String title, String pdfFile) {
    }
}