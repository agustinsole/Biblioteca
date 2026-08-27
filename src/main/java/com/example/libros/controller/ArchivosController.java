package com.example.libros.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/archivos")
public class ArchivosController {

    private static final Map<String, String> CARPETAS = new LinkedHashMap<>();
    static {
        CARPETAS.put("autores", "src/main/resources/static/img/autores");
        CARPETAS.put("libros", "src/main/resources/static/img/libros");
        CARPETAS.put("pdfs", "src/main/resources/static/pdfs");
        CARPETAS.put("videos", "src/main/resources/static/videos");
    }

    @GetMapping
    public String form(Model model) {
        cargarListados(model);
        return "archivos/form";
    }

    @PostMapping("/subir")
    public String subir(@RequestParam("carpeta") String carpeta,
                        @RequestParam("archivos") MultipartFile[] archivos,
                        Model model) {
        String rutaBase = CARPETAS.get(carpeta);
        if (rutaBase == null) {
            model.addAttribute("error", "Carpeta de destino no válida.");
            cargarListados(model);
            return "archivos/form";
        }

        Path folder = Path.of(rutaBase);
        int subidos = 0;
        try {
            Files.createDirectories(folder);
            for (MultipartFile archivo : archivos) {
                if (archivo == null || archivo.isEmpty()) {
                    continue;
                }
                String nombre = Path.of(archivo.getOriginalFilename() == null ? "" : archivo.getOriginalFilename())
                        .getFileName().toString();
                if (nombre.isBlank()) {
                    continue;
                }
                Path destino = folder.resolve(nombre).normalize();
                if (!destino.startsWith(folder)) {
                    continue;
                }
                archivo.transferTo(destino);
                subidos++;
            }
            model.addAttribute("mensaje", subidos + " archivo(s) subido(s) correctamente.");
        } catch (IOException e) {
            model.addAttribute("error", "Error al subir los archivos: " + e.getMessage());
        }

        cargarListados(model);
        return "archivos/form";
    }

    @PostMapping("/eliminar")
    public String eliminar(@RequestParam("carpeta") String carpeta,
                           @RequestParam(value = "seleccionados", required = false) List<String> seleccionados,
                           Model model) {
        String rutaBase = CARPETAS.get(carpeta);
        if (rutaBase == null) {
            model.addAttribute("error", "Carpeta de destino no válida.");
            cargarListados(model);
            return "archivos/form";
        }

        Path folder = Path.of(rutaBase).normalize();
        int eliminados = 0;
        if (seleccionados != null) {
            for (String nombre : seleccionados) {
                String nombreLimpio = Path.of(nombre).getFileName().toString();
                Path destino = folder.resolve(nombreLimpio).normalize();
                if (!destino.startsWith(folder)) {
                    continue;
                }
                try {
                    if (Files.deleteIfExists(destino)) {
                        eliminados++;
                    }
                } catch (IOException e) {
                    // se ignora el archivo que no se pudo eliminar y se continúa con el resto
                }
            }
        }
        model.addAttribute("mensaje", eliminados + " archivo(s) eliminado(s) correctamente.");

        cargarListados(model);
        return "archivos/form";
    }

    private void cargarListados(Model model) {
        model.addAttribute("archivosAutores", listarArchivos(CARPETAS.get("autores")));
        model.addAttribute("archivosLibros", listarArchivos(CARPETAS.get("libros")));
        model.addAttribute("archivosPdfs", listarArchivos(CARPETAS.get("pdfs")));
        model.addAttribute("archivosVideos", listarArchivos(CARPETAS.get("videos")));
    }

    private List<String> listarArchivos(String rutaBase) {
        Path folder = Path.of(rutaBase);
        try {
            if (!Files.isDirectory(folder)) {
                return List.of();
            }
            try (var stream = Files.list(folder)) {
                return stream.filter(Files::isRegularFile)
                        .map(Path::getFileName)
                        .map(Path::toString)
                        .sorted(Comparator.naturalOrder())
                        .toList();
            }
        } catch (IOException e) {
            return List.of();
        }
    }
}
