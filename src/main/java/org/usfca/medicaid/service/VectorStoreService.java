package org.usfca.medicaid.service;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.usfca.medicaid.config.AppConfig;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for managing document storage and retrieval in a vector database.
 */
public class VectorStoreService {
    
    private final EmbeddingStore<TextSegment> embeddingStore;
    private final EmbeddingModel embeddingModel;
    private final DocumentSplitter documentSplitter;
    
    /**
     * Constructs a new VectorStoreService with initialized embedding store,
     * embedding model, and document splitter.
     */
    public VectorStoreService() {
        this.embeddingStore = AppConfig.createPineconeEmbeddingStore();
        this.embeddingModel = AppConfig.createEmbeddingModel();
        this.documentSplitter = DocumentSplitters.recursive(300, 50);
    }
    
    /**
     * Add a document to the vector store.
     * Documents are split into segments, embedded, and stored with metadata.
     *
     * @param document the document to add to the vector store
     */
    public void addDocument(Document document) {
        String documentId = generateDocumentId(document);
        
        List<TextSegment> segments = documentSplitter.split(document);
        
        List<Embedding> embeddings = embeddingModel.embedAll(segments).content();
        
        for (int i = 0; i < segments.size(); i++) {
            TextSegment segment = segments.get(i);
            segment.metadata().put("document_id", documentId);
            segment.metadata().put("segment_index", String.valueOf(i));
        }
        
        embeddingStore.addAll(embeddings, segments);
        
        System.out.println("✅ Added " + segments.size() + " segments from document: " + document.metadata().toMap().get("title"));
    }
    
    /**
     * Add multiple documents to the vector store.
     *
     * @param documents the list of documents to add
     */
    public void addDocuments(List<Document> documents) {
        for (Document document : documents) {
            addDocument(document);
        }
    }
    
    /**
     * Clear all documents from the vector store.
     */
    public void clearAllDocuments() {
        embeddingStore.removeAll();
    }
    
    /**
     * Search for relevant documents based on a query.
     *
     * @param query the search query
     * @param maxResults the maximum number of results to return
     * @return a list of relevant text segments
     */
    public List<TextSegment> searchRelevantDocuments(String query, int maxResults) {
        Embedding queryEmbedding = embeddingModel.embed(query).content();
        
        EmbeddingSearchRequest searchRequest = EmbeddingSearchRequest.builder()
                .queryEmbedding(queryEmbedding)
                .maxResults(maxResults)
                .minScore(0.0)
                .build();
        
        EmbeddingSearchResult<TextSegment> searchResult = embeddingStore.search(searchRequest);
        
        return searchResult.matches().stream()
                .map(EmbeddingMatch::embedded)
                .collect(Collectors.toList());
    }
    
    /**
     * Search for relevant documents with a minimum score threshold.
     *
     * @param query the search query
     * @param maxResults the maximum number of results to return
     * @param minScore the minimum similarity score threshold (0.0 to 1.0)
     * @return a list of relevant text segments that meet the score threshold
     */
    public List<TextSegment> searchRelevantDocuments(String query, int maxResults, double minScore) {
        Embedding queryEmbedding = embeddingModel.embed(query).content();
        
        EmbeddingSearchRequest searchRequest = EmbeddingSearchRequest.builder()
                .queryEmbedding(queryEmbedding)
                .maxResults(maxResults)
                .minScore(minScore)
                .build();
        
        EmbeddingSearchResult<TextSegment> searchResult = embeddingStore.search(searchRequest);
        
        return searchResult.matches().stream()
                .map(EmbeddingMatch::embedded)
                .collect(Collectors.toList());
    }
    
    /**
     * Get all documents in the store.
     * Uses a broad query to retrieve up to 1000 documents.
     *
     * @return a list of all text segments in the store
     */
    public List<TextSegment> getAllDocuments() {
        return searchRelevantDocuments("medicaid eligibility benefits", 1000);
    }
    
    /**
     * Generate a stable document ID based on source URL/path only.
     *
     * @param document the document to generate an ID for
     * @return a stable unique identifier for the document
     */
    private String generateDocumentId(Document document) {
        String source = (String) document.metadata().toMap().get("source");
        String title = (String) document.metadata().toMap().get("title");

        String cleanTitle = title.replaceAll("[^a-zA-Z0-9]", "_");
        String documentId = source + "_" + cleanTitle;

        return documentId;
    }
}
