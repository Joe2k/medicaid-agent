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
        
        // Check if document already exists (if configured to skip)
        if (AppConfig.isSkipExistingDocuments() && documentExists(documentId)) {
            System.out.println("⏭️  Document already exists, skipping: " + document.metadata().asMap().get("title"));
            return;
        }
        
        // Split the document into smaller segments
        List<TextSegment> segments = documentSplitter.split(document);
        
        // Generate embeddings for each segment
        List<Embedding> embeddings = embeddingModel.embedAll(segments).content();
        
        // Add document ID to segments for tracking
        for (int i = 0; i < segments.size(); i++) {
            TextSegment segment = segments.get(i);
            segment.metadata().put("document_id", documentId);
            segment.metadata().put("segment_index", String.valueOf(i));
        }
        
        // Add to vector store
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
        // Generate embedding for the query
        Embedding queryEmbedding = embeddingModel.embed(query).content();
        
        // Search for similar embeddings
        List<EmbeddingMatch<TextSegment>> matches = embeddingStore.findRelevant(queryEmbedding, maxResults, 0.0);
        
        // Extract text segments from matches
        return matches.stream()
                .map(EmbeddingMatch::embedded)
                .collect(Collectors.toList());
    }
    
    /**
     * Search for relevant documents with a minimum score threshold
     */
    public List<TextSegment> searchRelevantDocuments(String query, int maxResults, double minScore) {
        // Generate embedding for the query
        Embedding queryEmbedding = embeddingModel.embed(query).content();
        
        // Search for similar embeddings
        List<EmbeddingMatch<TextSegment>> matches = embeddingStore.findRelevant(queryEmbedding, maxResults, 0.0);
        
        // Filter by minimum score and extract text segments
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
     * Generate a unique document ID based on source and content hash
     */
    private String generateDocumentId(Document document) {
        String source = document.metadata().asMap().get("source");
        String title = document.metadata().asMap().get("title");
        
        // Create a hash of the content for uniqueness
        String contentHash = String.valueOf(document.text().hashCode());
        
        return source + "_" + title.replaceAll("[^a-zA-Z0-9]", "_") + "_" + contentHash;
    }
    
    /**
     * Check if a document already exists in the vector store
     */
    private boolean documentExists(String documentId) {
        try {
            // Search for any segments with this document ID
            List<EmbeddingMatch<TextSegment>> matches = embeddingStore.findRelevant(
                embeddingModel.embed(documentId).content(), 
                1, 
                0.0
            );
            
            // Check if any match has the same document ID
            for (EmbeddingMatch<TextSegment> match : matches) {
                String existingDocId = match.embedded().metadata().asMap().get("document_id");
                if (documentId.equals(existingDocId)) {
                    return true;
                }
            }
            
            return false;
        } catch (Exception e) {
            // If there's an error checking, assume document doesn't exist
            System.out.println("Warning: Could not check if document exists: " + e.getMessage());
            return false;
        }
    }
}
