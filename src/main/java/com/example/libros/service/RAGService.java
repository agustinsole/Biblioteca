package com.example.libros.service;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.io.IOException;
import java.io.InputStream;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Pattern;

@Service
public class RAGService {

    private static final Pattern NON_ALNUM = Pattern.compile("[^a-z0-9 ]");
    private static final Pattern MULTI_SPACE = Pattern.compile("\\s+");
        private static final Set<String> STOPWORDS = Set.of(
            "el", "la", "los", "las", "un", "una", "unos", "unas",
            "de", "del", "al", "a", "en", "con", "por", "para", "sin", "sobre",
            "que", "como", "cual", "cuales", "quien", "quienes", "donde", "cuando",
            "y", "o", "u", "es", "son", "se", "su", "sus", "le", "les"
        );

    private final RestClient iaRestClient;
    private final boolean enabled;
    private final String apiKey;
    private final String model;
    private final String endpoint;
    private final int maxContextChars;
    private final PathMatchingResourcePatternResolver resourceResolver;
    private final String pdfPattern;
    private final ConcurrentMap<String, String> pdfTextCache;

    public RAGService(RestClient.Builder restClientBuilder,
                      @Value("${ai.enabled:false}") boolean enabled,
                      @Value("${ai.base-url:https://api.openai.com/v1}") String baseUrl,
                      @Value("${ai.api-key:}") String apiKey,
                      @Value("${ai.model:gpt-4o-mini}") String model,
                      @Value("${ai.endpoint:/chat/completions}") String endpoint,
                      @Value("${ai.rag.max-context-chars:12000}") int maxContextChars,
                      @Value("${ai.rag.pdf-pattern:classpath*:static/pdfs/*.pdf}") String pdfPattern) {
        this.iaRestClient = restClientBuilder
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
        this.enabled = enabled;
        this.apiKey = apiKey;
        this.model = model;
        this.endpoint = endpoint;
        this.maxContextChars = maxContextChars;
        this.pdfPattern = pdfPattern;
        this.resourceResolver = new PathMatchingResourcePatternResolver();
        this.pdfTextCache = new ConcurrentHashMap<>();
    }

    public List<String> listarPdfsDisponibles() {
        try {
            Resource[] resources = resourceResolver.getResources(pdfPattern);
            List<String> nombres = new ArrayList<>();

            for (Resource resource : resources) {
                String filename = resource.getFilename();
                if (filename != null && filename.toLowerCase(Locale.ROOT).endsWith(".pdf")) {
                    nombres.add(filename);
                }
            }

            nombres.sort(String.CASE_INSENSITIVE_ORDER);
            return nombres;
        } catch (IOException ex) {
            throw new IllegalStateException("No se pudo listar la carpeta de PDFs configurada en ai.rag.pdf-pattern.", ex);
        }
    }

    public String responderPreguntaSobrePdf(String pdfFile, String pregunta) {
        if (!enabled) {
            return "IA deshabilitada. Activa ai.enabled=true en application.properties.";
        }
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("Falta configurar ai.api-key en application.properties.");
        }

        String textoPdf = cargarTextoPdf(pdfFile);
        if (textoPdf.isBlank()) {
            return "No se pudo extraer texto del PDF seleccionado.";
        }

        String contexto = construirContextoRag(textoPdf, pregunta);
        String prompt = construirPromptRag(pdfFile, pregunta, contexto);

        IAService.ChatCompletionRequest request = new IAService.ChatCompletionRequest(
                model,
                List.of(new IAService.ChatMessage("user", prompt)),
                0.2
        );

        try {
            IAService.ChatCompletionResponse response = iaRestClient.post()
                    .uri(endpoint)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .body(request)
                    .retrieve()
                    .body(IAService.ChatCompletionResponse.class);

            if (response == null || response.choices() == null || response.choices().isEmpty()) {
                return "La IA no devolvio contenido.";
            }

            IAService.ChatChoice firstChoice = response.choices().getFirst();
            if (firstChoice.message() == null || firstChoice.message().content() == null) {
                return "La IA devolvio una respuesta vacia.";
            }

            return firstChoice.message().content().trim();
        } catch (RestClientResponseException ex) {
            throw new IllegalStateException("Error HTTP al invocar IA: " + ex.getStatusCode() + " - " + ex.getResponseBodyAsString(), ex);
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudo invocar el servicio de IA para RAG.", ex);
        }
    }

    private String cargarTextoPdf(String pdfFile) {
        String key = pdfFile == null ? "" : pdfFile.trim();
        if (key.isBlank()) {
            throw new IllegalStateException("Debes seleccionar un archivo PDF.");
        }

        return pdfTextCache.computeIfAbsent(key, this::extraerTextoPdfSeguro);
    }

    private String extraerTextoPdfSeguro(String pdfFile) {
        Resource resource = buscarRecursoPorNombre(pdfFile);
        if (resource == null) {
            throw new IllegalStateException("El archivo PDF seleccionado no existe en resources/static/pdfs.");
        }

        try (InputStream inputStream = resource.getInputStream();
             PDDocument document = Loader.loadPDF(inputStream.readAllBytes())) {
            PDFTextStripper stripper = new PDFTextStripper();
            String texto = stripper.getText(document);
            return texto == null ? "" : texto.trim();
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudo leer el PDF '" + pdfFile + "'.", ex);
        }
    }

    private Resource buscarRecursoPorNombre(String pdfFile) {
        try {
            Resource[] resources = resourceResolver.getResources(pdfPattern);
            for (Resource resource : resources) {
                String filename = resource.getFilename();
                if (filename != null && filename.equalsIgnoreCase(pdfFile)) {
                    return resource;
                }
            }
            return null;
        } catch (IOException ex) {
            throw new IllegalStateException("No se pudo acceder a la carpeta de PDFs.", ex);
        }
    }

    private String construirContextoRag(String textoPdf, String pregunta) {
        List<String> chunks = dividirEnChunks(textoPdf, 1400, 250);
        if (chunks.isEmpty()) {
            return "";
        }

        String preguntaNorm = normalizar(pregunta);
        if (esPreguntaResumen(preguntaNorm)) {
            return construirContextoResumen(chunks);
        }

        Set<String> keywords = extraerKeywords(preguntaNorm);
        List<ChunkScore> scores = new ArrayList<>();
        for (int i = 0; i < chunks.size(); i++) {
            String chunkNorm = normalizar(chunks.get(i));
            int score = puntuarChunk(chunkNorm, preguntaNorm, keywords);
            scores.add(new ChunkScore(i, score));
        }

        scores.sort(Comparator.comparingInt(ChunkScore::score).reversed());

        List<Integer> base = new ArrayList<>();
        for (ChunkScore item : scores) {
            if (item.score() <= 0) {
                continue;
            }
            base.add(item.index());
            if (base.size() >= 6) {
                break;
            }
        }

        if (base.isEmpty()) {
            return construirContextoResumen(chunks);
        }

        LinkedHashSet<Integer> indices = new LinkedHashSet<>();
        for (Integer idx : base) {
            indices.add(idx);
            if (idx > 0) {
                indices.add(idx - 1);
            }
            if (idx < chunks.size() - 1) {
                indices.add(idx + 1);
            }
            if (indices.size() >= 10) {
                break;
            }
        }

        StringBuilder contexto = new StringBuilder();
        int frag = 1;
        for (Integer idx : indices) {
            contexto.append("[FRAGMENTO_").append(frag).append("]\\n")
                    .append(chunks.get(idx))
                    .append("\\n\\n");
            frag++;
            if (contexto.length() >= maxContextChars) {
                break;
            }
        }

        if (contexto.length() == 0) {
            return recortar(textoPdf, maxContextChars);
        }

        return recortar(contexto.toString(), maxContextChars);
    }

    private List<String> dividirEnChunks(String texto, int chunkSize, int overlap) {
        List<String> chunks = new ArrayList<>();
        String limpio = texto == null ? "" : texto.replace("\r", " ").trim();
        if (limpio.isBlank()) {
            return chunks;
        }

        int inicio = 0;
        while (inicio < limpio.length()) {
            int fin = Math.min(limpio.length(), inicio + chunkSize);
            String chunk = limpio.substring(inicio, fin).trim();
            if (!chunk.isBlank()) {
                chunks.add(chunk);
            }
            if (fin >= limpio.length()) {
                break;
            }
            inicio = Math.max(fin - overlap, inicio + 1);
        }
        return chunks;
    }

    private int puntuarChunk(String chunkNorm, String preguntaNorm, Set<String> keywords) {
        int score = 0;

        if (!preguntaNorm.isBlank() && chunkNorm.contains(preguntaNorm)) {
            score += 12;
        }

        int keywordHits = 0;
        for (String keyword : keywords) {
            if (chunkNorm.contains(keyword)) {
                keywordHits++;
                score += 2;
            }
        }

        if (keywordHits >= 2) {
            score += 3;
        }

        String[] tokens = preguntaNorm.split(" ");
        for (int i = 0; i < tokens.length - 1; i++) {
            String a = tokens[i];
            String b = tokens[i + 1];
            if (a.length() < 3 || b.length() < 3) {
                continue;
            }
            String bigrama = a + " " + b;
            if (chunkNorm.contains(bigrama)) {
                score += 3;
            }
        }

        return score;
    }

    private Set<String> extraerKeywords(String textoNorm) {
        Set<String> keywords = new HashSet<>();
        if (textoNorm == null || textoNorm.isBlank()) {
            return keywords;
        }

        for (String token : textoNorm.split(" ")) {
            if (token.length() >= 3 && !STOPWORDS.contains(token)) {
                keywords.add(token);
            }
        }
        return keywords;
    }

    private boolean esPreguntaResumen(String preguntaNorm) {
        return preguntaNorm.contains("resumen")
                || preguntaNorm.contains("sinopsis")
                || preguntaNorm.contains("de que trata")
                || preguntaNorm.contains("de que va")
                || preguntaNorm.contains("tema principal");
    }

    private String construirContextoResumen(List<String> chunks) {
        if (chunks.isEmpty()) {
            return "";
        }

        int total = chunks.size();
        LinkedHashSet<Integer> indices = new LinkedHashSet<>();
        indices.add(0);
        indices.add(Math.max(0, total / 4));
        indices.add(Math.max(0, total / 2));
        indices.add(Math.max(0, (total * 3) / 4));
        indices.add(total - 1);

        StringBuilder contexto = new StringBuilder();
        int frag = 1;
        for (Integer idx : indices) {
            if (idx < 0 || idx >= total) {
                continue;
            }
            contexto.append("[FRAGMENTO_").append(frag).append("]\\n")
                    .append(chunks.get(idx))
                    .append("\\n\\n");
            frag++;
            if (contexto.length() >= maxContextChars) {
                break;
            }
        }

        return recortar(contexto.toString(), maxContextChars);
    }

    private String normalizar(String texto) {
        if (texto == null) {
            return "";
        }
        String sinAcentos = Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
        String limpio = NON_ALNUM.matcher(sinAcentos.toLowerCase(Locale.ROOT)).replaceAll(" ");
        return MULTI_SPACE.matcher(limpio).replaceAll(" ").trim();
    }

    private String construirPromptRag(String pdfFile, String pregunta, String contexto) {
        return "Responde SOLO con informacion del CONTEXTO_PDF. "
            + "Si la pregunta pide un resumen, redacta un resumen breve usando los fragmentos disponibles. "
            + "Solo si realmente no hay evidencia en los fragmentos para responder, di exactamente: 'No encuentro esa informacion en este PDF'.\\n\\n"
                + "PDF_SELECCIONADO:\\n" + pdfFile + "\\n\\n"
                + "CONTEXTO_PDF:\\n" + contexto + "\\n\\n"
                + "PREGUNTA_USUARIO:\\n" + pregunta;
    }

    private String recortar(String texto, int maxChars) {
        if (texto == null) {
            return "";
        }
        if (texto.length() <= maxChars) {
            return texto;
        }
        return texto.substring(0, maxChars) + "\\n... [contexto RAG truncado]";
    }

    private record ChunkScore(int index, int score) {
    }
}
