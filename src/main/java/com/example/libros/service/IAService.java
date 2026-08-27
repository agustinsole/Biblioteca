package com.example.libros.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.List;

@Service
public class IAService {

    private final RestClient iaRestClient;
    private final RestClient apiRestClient;
    private final String apiKey;
    private final String model;
    private final String endpoint;
    private final boolean enabled;
    private final String autoresDtoUrl;
    private final int maxAutoresContextChars;
    private final boolean webFallbackEnabled;
    private final String wikipediaApiUrl;
    private final ObjectMapper objectMapper;

    public IAService(RestClient.Builder restClientBuilder,
                     @Value("${ai.base-url:https://api.openai.com/v1}") String baseUrl,
                     @Value("${ai.api-key:}") String apiKey,
                     @Value("${ai.model:gpt-4o-mini}") String model,
                     @Value("${ai.endpoint:/chat/completions}") String endpoint,
                 @Value("${ai.enabled:false}") boolean enabled,
                 @Value("${ai.autores-dto-url:http://localhost:8080/api/autores/dto}") String autoresDtoUrl,
                 @Value("${ai.max-autores-context-chars:12000}") int maxAutoresContextChars,
                 @Value("${ai.web-fallback-enabled:true}") boolean webFallbackEnabled,
                 @Value("${ai.wikipedia-api-url:https://es.wikipedia.org/w/api.php}") String wikipediaApiUrl) {
        this.iaRestClient = restClientBuilder
            .baseUrl(baseUrl)
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .build();
        this.apiRestClient = restClientBuilder
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .build();
        this.apiKey = apiKey;
        this.model = model;
        this.endpoint = endpoint;
        this.enabled = enabled;
        this.autoresDtoUrl = autoresDtoUrl;
        this.maxAutoresContextChars = maxAutoresContextChars;
        this.webFallbackEnabled = webFallbackEnabled;
        this.wikipediaApiUrl = wikipediaApiUrl;
        this.objectMapper = new ObjectMapper();
    }

    public String generarRespuesta(String prompt) {
        if (!enabled) {
            return "IA deshabilitada. Activa ai.enabled=true en application.properties.";
        }
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("Falta configurar ai.api-key en application.properties.");
        }

        String autoresJsonCrudo = cargarAutoresDtoCrudo();
        String autoresContexto = construirContextoAutores(prompt, autoresJsonCrudo);
        String contextoWeb = cargarContextoWeb(prompt);
        String promptEnriquecido = construirPromptConContextos(prompt, autoresContexto, contextoWeb);

        ChatCompletionRequest request = new ChatCompletionRequest(
                model,
            List.of(new ChatMessage("user", promptEnriquecido)),
                0.7
        );

        try {
            ChatCompletionResponse response = iaRestClient.post()
                    .uri(endpoint)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .body(request)
                    .retrieve()
                    .body(ChatCompletionResponse.class);

            if (response == null || response.choices() == null || response.choices().isEmpty()) {
                return "La IA no devolvio contenido.";
            }

            ChatChoice firstChoice = response.choices().getFirst();
            if (firstChoice.message() == null || firstChoice.message().content() == null) {
                return "La IA devolvio una respuesta vacia.";
            }

            return firstChoice.message().content().trim();
        } catch (RestClientResponseException ex) {
            throw new IllegalStateException("Error HTTP al invocar IA: " + ex.getStatusCode() + " - " + ex.getResponseBodyAsString(), ex);
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudo invocar el servicio de IA.", ex);
        }
    }

    public record ChatCompletionRequest(String model, List<ChatMessage> messages, double temperature) {
    }

    public record ChatMessage(String role, String content) {
    }

    public record ChatCompletionResponse(List<ChatChoice> choices) {
    }

    public record ChatChoice(ChatMessage message) {
    }

    private String cargarAutoresDtoCrudo() {
        try {
            String body = apiRestClient.get()
                    .uri(autoresDtoUrl)
                    .retrieve()
                    .body(String.class);

            if (body == null || body.isBlank()) {
                return "[]";
            }

            return body.trim();
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudo leer la API de autores DTO en " + autoresDtoUrl + ".", ex);
        }
    }

    private String construirContextoAutores(String promptUsuario, String autoresJsonCrudo) {
        StringBuilder contexto = new StringBuilder();
        contexto.append("ESQUEMA_IMPORTANTE: Cada autor usa nomAutor, apellido1 y apellido2. ")
                .append("Para identificar autores, combina esos tres campos. ")
                .append("Los libros estan en libros[].nomLibro.\\n\\n");

        String autoresRelevantes = extraerAutoresRelevantes(promptUsuario, autoresJsonCrudo);
        if (!autoresRelevantes.isBlank()) {
            contexto.append("AUTORES_RELEVANTES_JSON:\\n")
                    .append(autoresRelevantes)
                    .append("\\n\\n");
        }

        contexto.append("AUTORES_DTO_JSON_COMPLETO_TRUNCADO:\\n")
                .append(limitarContexto(autoresJsonCrudo));

        return contexto.toString();
    }

    private String extraerAutoresRelevantes(String promptUsuario, String autoresJsonCrudo) {
        try {
            JsonNode root = objectMapper.readTree(autoresJsonCrudo);
            if (!root.isArray()) {
                return "";
            }

            String promptNormalizado = normalizar(promptUsuario);
            ArrayNode relevantes = objectMapper.createArrayNode();

            for (JsonNode autor : root) {
                String nombre = autor.path("nomAutor").asText("");
                String apellido1 = autor.path("apellido1").asText("");
                String apellido2 = autor.path("apellido2").asText("");

                String nombreN = normalizar(nombre);
                String apellido1N = normalizar(apellido1);
                String apellido2N = normalizar(apellido2);
                String nombreCompletoN = (nombreN + " " + apellido1N + " " + apellido2N).trim().replaceAll("\\s+", " ");

                int score = 0;

                if (!nombreCompletoN.isBlank() && promptNormalizado.contains(nombreCompletoN)) {
                    score += 5;
                }
                if (!nombreN.isBlank() && promptNormalizado.contains(nombreN)) {
                    score += 1;
                }
                if (!apellido1N.isBlank() && promptNormalizado.contains(apellido1N)) {
                    score += 2;
                }
                if (!apellido2N.isBlank() && promptNormalizado.contains(apellido2N)) {
                    score += 2;
                }
                if (!apellido1N.isBlank() && !apellido2N.isBlank()
                        && promptNormalizado.contains(apellido1N + " " + apellido2N)) {
                    score += 3;
                }

                if (score >= 2) {
                    relevantes.add(autor);
                    if (relevantes.size() >= 5) {
                        break;
                    }
                }
            }

            if (relevantes.isEmpty()) {
                return "";
            }

            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(relevantes);
        } catch (Exception ex) {
            return "";
        }
    }

    private String normalizar(String texto) {
        if (texto == null) {
            return "";
        }
        String sinAcentos = Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
        return sinAcentos.toLowerCase().replaceAll("[^a-z0-9 ]", " ").replaceAll("\\s+", " ").trim();
    }

    private String construirPromptConContextos(String promptUsuario, String autoresJson, String contextoWeb) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Usa primero los datos del JSON para responder. ")
            .append("IMPORTANTE: el nombre del autor puede estar repartido en nomAutor, apellido1 y apellido2. ")
            .append("Si no encuentras la respuesta en el JSON, usa el contexto web. ")
            .append("Los datos del JSON son de los libros que he leido.\\n\\n")
                .append("DATOS_JSON:\\n")
                .append(autoresJson)
                .append("\\n\\n");

        if (contextoWeb != null && !contextoWeb.isBlank()) {
            prompt.append("CONTEXTO_WEB:\\n")
                    .append(contextoWeb)
                    .append("\\n\\n");
        }

        prompt.append("PREGUNTA_USUARIO:\\n")
                .append(promptUsuario);
        return prompt.toString();
    }

    private String limitarContexto(String texto) {
        if (texto.length() <= maxAutoresContextChars) {
            return texto;
        }
        return texto.substring(0, maxAutoresContextChars) + "\\n... [contexto truncado]";
    }

    private String cargarContextoWeb(String promptUsuario) {
        if (!webFallbackEnabled) {
            return "";
        }

        try {
            String consulta = URLEncoder.encode(promptUsuario, StandardCharsets.UTF_8);
            String searchResponse = apiRestClient.get()
                    .uri(wikipediaApiUrl + "?action=query&list=search&srsearch=" + consulta + "&format=json&utf8=1&origin=*")
                    .retrieve()
                    .body(String.class);

            if (searchResponse == null || searchResponse.isBlank()) {
                return "";
            }

            JsonNode root = objectMapper.readTree(searchResponse);
            JsonNode searchArray = root.path("query").path("search");
            if (!searchArray.isArray() || searchArray.isEmpty()) {
                return "";
            }

            String title = searchArray.get(0).path("title").asText("");
            if (title.isBlank()) {
                return "";
            }

            String titleEncoded = URLEncoder.encode(title, StandardCharsets.UTF_8);
            String extractResponse = apiRestClient.get()
                    .uri(wikipediaApiUrl + "?action=query&prop=extracts&explaintext=1&redirects=1&format=json&utf8=1&origin=*&titles=" + titleEncoded)
                    .retrieve()
                    .body(String.class);

            if (extractResponse == null || extractResponse.isBlank()) {
                return "";
            }

            JsonNode extractRoot = objectMapper.readTree(extractResponse);
            JsonNode pages = extractRoot.path("query").path("pages");
            if (!pages.isObject()) {
                return "";
            }

            JsonNode page = pages.elements().hasNext() ? pages.elements().next() : null;
            if (page == null) {
                return "";
            }

            String extract = page.path("extract").asText("");
            if (extract.isBlank()) {
                return "";
            }

            return "Wikipedia: " + limitarContexto(extract);
        } catch (Exception ex) {
            return "";
        }
    }
}