package org.usfca.medicaid.service;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.usfca.medicaid.config.AppConfig;

import java.util.List;
import java.util.stream.Collectors;

public class VectorStoreService {
    
    private final EmbeddingStore<TextSegment> embeddingStore;
    private final EmbeddingModel embeddingModel;
    private final DocumentSplitter documentSplitter;
    
    public VectorStoreService() {
        this.embeddingStore = AppConfig.createPineconeEmbeddingStore();
        this.embeddingModel = AppConfig.createEmbeddingModel();
        this.documentSplitter = DocumentSplitters.recursive(300, 50);
    }
    
    /**
     * Add a document to the vector store (only if not already present)
     */
    public void addDocument(Document document) {
        String documentId = generateDocumentId(document);
        
        if (AppConfig.isSkipExistingDocuments() && documentExists(documentId)) {
            System.out.println("⏭️  Document already exists, skipping: " + document.metadata().asMap().get("title"));
            return;
        }
        
        List<TextSegment> segments = documentSplitter.split(document);
        
        List<Embedding> embeddings = embeddingModel.embedAll(segments).content();
        
        for (int i = 0; i < segments.size(); i++) {
            TextSegment segment = segments.get(i);
            segment.metadata().put("document_id", documentId);
            segment.metadata().put("segment_index", String.valueOf(i));
        }
        
        embeddingStore.addAll(embeddings, segments);
        
        System.out.println("✅ Added " + segments.size() + " segments from document: " + document.metadata().asMap().get("title"));
    }
    
    /**
     * Add multiple documents to the vector store
     */
    public void addDocuments(List<Document> documents) {
        for (Document document : documents) {
            addDocument(document);
        }
    }
    
    /**
     * Search for relevant documents based on a query
     */
    public List<TextSegment> searchRelevantDocuments(String query, int maxResults) {
        Embedding queryEmbedding = embeddingModel.embed(query).content();
        
        List<EmbeddingMatch<TextSegment>> matches = embeddingStore.findRelevant(queryEmbedding, maxResults, 0.0);
        
        return matches.stream()
                .map(EmbeddingMatch::embedded)
                .collect(Collectors.toList());
    }
    
    /**
     * Search for relevant documents with a minimum score threshold
     */
    public List<TextSegment> searchRelevantDocuments(String query, int maxResults, double minScore) {
        Embedding queryEmbedding = embeddingModel.embed(query).content();
        
        List<EmbeddingMatch<TextSegment>> matches = embeddingStore.findRelevant(queryEmbedding, maxResults, 0.0);
        
        return matches.stream()
                .filter(match -> match.score() >= minScore)
                .map(EmbeddingMatch::embedded)
                .collect(Collectors.toList());
    }
    
    /**
     * Get all documents in the store
     */
    public List<TextSegment> getAllDocuments() {
        return searchRelevantDocuments("medicaid eligibility benefits", 1000);
    }
    
    /**
     * Generate a stable document ID based on source URL/path only
     * This ensures the same document always gets the same ID, enabling duplicate detection
     */
    private String generateDocumentId(Document document) {
        String source = document.metadata().asMap().get("source");
        String title = document.metadata().asMap().get("title");

        String cleanTitle = title.replaceAll("[^a-zA-Z0-9]", "_");
        String documentId = source + "_" + cleanTitle;

        return documentId;
    }
    
    /**
     * Check if a document already exists in the vector store
     */
    private boolean documentExists(String documentId) {
        try {
            List<EmbeddingMatch<TextSegment>> matches = embeddingStore.findRelevant(
                embeddingModel.embed(documentId).content(), 
                1, 
                0.0
            );
            
            for (EmbeddingMatch<TextSegment> match : matches) {
                String existingDocId = match.embedded().metadata().asMap().get("document_id");
                if (documentId.equals(existingDocId)) {
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            System.out.println("Warning: Could not check if document exists: " + e.getMessage());
            return false;
        }
    }
}
