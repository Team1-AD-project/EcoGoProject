package com.example.EcoGo.service.chatbot;

import com.example.EcoGo.dto.chatbot.ChatResponseDto;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/**
 * RAG (Retrieval-Augmented Generation) service.
 * Loads knowledge chunks from chunks.jsonl and performs TF-IDF based retrieval.
 * Pure Java implementation — no external ML libraries needed.
 */
@Service
public class RagService {

    private static final Logger log = LoggerFactory.getLogger(RagService.class);

    private final ObjectMapper objectMapper = new ObjectMapper();

    // Loaded knowledge chunks
    private final List<KnowledgeChunk> chunks = new ArrayList<>();

    // TF-IDF data structures
    private final Map<String, Map<String, Double>> tfidfVectors = new HashMap<>(); // chunkId -> {term -> tfidf}
    private final Map<String, Double> idfValues = new HashMap<>(); // term -> idf

    @PostConstruct
    public void init() {
        try {
            loadChunks();
            buildTfidfIndex();
            log.info("RAG service initialized with {} chunks", chunks.size());
        } catch (Exception e) {
            log.warn("Failed to initialize RAG service: {}. Chat will use default answers.", e.getMessage());
        }
    }

    private void loadChunks() throws Exception {
        ClassPathResource resource = new ClassPathResource("chatbot/chunks.jsonl");
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) continue;
                JsonNode node = objectMapper.readTree(line);
                chunks.add(new KnowledgeChunk(
                        node.get("chunk_id").asText(),
                        node.get("source").asText(),
                        node.get("title").asText(),
                        node.get("text").asText()
                ));
            }
        }
    }

    private void buildTfidfIndex() {
        // Step 1: Compute term frequencies for each chunk
        Map<String, Map<String, Integer>> termFreqs = new HashMap<>();
        Map<String, Integer> docFreqs = new HashMap<>(); // term -> number of docs containing it

        for (KnowledgeChunk chunk : chunks) {
            List<String> tokens = tokenize(chunk.text());
            Map<String, Integer> tf = new HashMap<>();
            Set<String> uniqueTerms = new HashSet<>();

            for (String token : tokens) {
                tf.merge(token, 1, Integer::sum);
                uniqueTerms.add(token);
            }

            termFreqs.put(chunk.chunkId(), tf);

            for (String term : uniqueTerms) {
                docFreqs.merge(term, 1, Integer::sum);
            }
        }

        // Step 2: Compute IDF
        int totalDocs = chunks.size();
        for (Map.Entry<String, Integer> entry : docFreqs.entrySet()) {
            double idf = Math.log((double) (totalDocs + 1) / (entry.getValue() + 1)) + 1.0;
            idfValues.put(entry.getKey(), idf);
        }

        // Step 3: Compute TF-IDF vectors
        for (KnowledgeChunk chunk : chunks) {
            Map<String, Integer> tf = termFreqs.get(chunk.chunkId());
            Map<String, Double> tfidf = new HashMap<>();
            int maxTf = tf.values().stream().max(Integer::compareTo).orElse(1);

            for (Map.Entry<String, Integer> entry : tf.entrySet()) {
                double normalizedTf = 0.5 + 0.5 * entry.getValue() / maxTf;
                double idf = idfValues.getOrDefault(entry.getKey(), 1.0);
                tfidf.put(entry.getKey(), normalizedTf * idf);
            }

            tfidfVectors.put(chunk.chunkId(), tfidf);
        }
    }

    /**
     * Retrieve top-k relevant chunks for the given query.
     */
    public List<ChatResponseDto.Citation> retrieve(String query, int k) {
        if (chunks.isEmpty()) {
            return Collections.emptyList();
        }

        // Build query TF-IDF vector
        List<String> queryTokens = tokenize(query);
        Map<String, Integer> queryTf = new HashMap<>();
        for (String token : queryTokens) {
            queryTf.merge(token, 1, Integer::sum);
        }

        int maxTf = queryTf.values().stream().max(Integer::compareTo).orElse(1);
        Map<String, Double> queryVector = new HashMap<>();
        for (Map.Entry<String, Integer> entry : queryTf.entrySet()) {
            double normalizedTf = 0.5 + 0.5 * entry.getValue() / maxTf;
            double idf = idfValues.getOrDefault(entry.getKey(), 1.0);
            queryVector.put(entry.getKey(), normalizedTf * idf);
        }

        // Compute cosine similarity with each chunk
        List<ScoredChunk> scored = new ArrayList<>();
        for (KnowledgeChunk chunk : chunks) {
            Map<String, Double> chunkVector = tfidfVectors.get(chunk.chunkId());
            double similarity = cosineSimilarity(queryVector, chunkVector);
            scored.add(new ScoredChunk(chunk, similarity));
        }

        // Sort by score descending, return top-k
        scored.sort((a, b) -> Double.compare(b.score(), a.score()));

        return scored.stream()
                .limit(k)
                .filter(s -> s.score() > 0.0)
                .map(s -> new ChatResponseDto.Citation(
                        s.chunk().title(),
                        s.chunk().source(),
                        s.chunk().text().length() > 240
                                ? s.chunk().text().substring(0, 240)
                                : s.chunk().text()
                ))
                .collect(Collectors.toList());
    }

    public boolean isAvailable() {
        return !chunks.isEmpty();
    }

    // --- Utilities ---

    private List<String> tokenize(String text) {
        // Simple tokenizer: lowercase, split on non-word characters, filter short tokens
        // Supports both Chinese characters and English words
        List<String> tokens = new ArrayList<>();

        // Extract Chinese characters (each character as a token for unigram)
        // and English words
        String lower = text.toLowerCase();

        // Split into segments: Chinese chars individually, English words as groups
        StringBuilder current = new StringBuilder();
        for (int i = 0; i < lower.length(); i++) {
            char c = lower.charAt(i);
            if (isChinese(c)) {
                // Flush any English word
                if (current.length() > 0) {
                    String word = current.toString().trim();
                    if (word.length() >= 2) tokens.add(word);
                    current.setLength(0);
                }
                // Add Chinese char as token
                tokens.add(String.valueOf(c));

                // Also add bigrams for Chinese
                if (i + 1 < lower.length() && isChinese(lower.charAt(i + 1))) {
                    tokens.add("" + c + lower.charAt(i + 1));
                }
            } else if (Character.isLetterOrDigit(c)) {
                current.append(c);
            } else {
                if (current.length() > 0) {
                    String word = current.toString().trim();
                    if (word.length() >= 2) tokens.add(word);
                    current.setLength(0);
                }
            }
        }

        if (current.length() > 0) {
            String word = current.toString().trim();
            if (word.length() >= 2) tokens.add(word);
        }

        return tokens;
    }

    private boolean isChinese(char c) {
        Character.UnicodeBlock block = Character.UnicodeBlock.of(c);
        return block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
                || block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A
                || block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B
                || block == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS;
    }

    private double cosineSimilarity(Map<String, Double> a, Map<String, Double> b) {
        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (Map.Entry<String, Double> entry : a.entrySet()) {
            double valA = entry.getValue();
            normA += valA * valA;
            Double valB = b.get(entry.getKey());
            if (valB != null) {
                dotProduct += valA * valB;
            }
        }

        for (double valB : b.values()) {
            normB += valB * valB;
        }

        if (normA == 0.0 || normB == 0.0) return 0.0;
        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    // --- Internal records ---

    private record KnowledgeChunk(String chunkId, String source, String title, String text) {}

    private record ScoredChunk(KnowledgeChunk chunk, double score) {}
}
