package org.usfca.medicaid.config;

import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.store.embedding.pinecone.PineconeEmbeddingStore;

import java.time.Duration;

public class AppConfig {
    
    private static final String OPENAI_API_KEY = System.getenv("OPENAI_API_KEY");
    private static final String PINECONE_API_KEY = System.getenv("PINECONE_API_KEY");
    private static final String PINECONE_ENVIRONMENT = System.getenv("PINECONE_ENVIRONMENT") != null ? 
        System.getenv("PINECONE_ENVIRONMENT") : "us-east-1-aws";
    private static final String PINECONE_INDEX_NAME = System.getenv("PINECONE_INDEX_NAME") != null ? 
        System.getenv("PINECONE_INDEX_NAME") : "medicaid-minnesota";
    
    // Vector Store Configuration
    /**
     * Whether to skip documents that already exist in the vector store
     * Set to false if you want to force re-upload all documents
     */
    private static final boolean SKIP_EXISTING_DOCUMENTS = true;

    
    public static OpenAiChatModel createChatModel() {
        if (OPENAI_API_KEY == null || OPENAI_API_KEY.isEmpty()) {
            throw new IllegalStateException("OPENAI_API_KEY environment variable is required");
        }
        
        return OpenAiChatModel.builder()
                .apiKey(OPENAI_API_KEY)
                .modelName("gpt-3.5-turbo")
                .temperature(0.1)
                .timeout(Duration.ofSeconds(60))
                .build();
    }
    
    public static EmbeddingModel createEmbeddingModel() {
        if (OPENAI_API_KEY == null || OPENAI_API_KEY.isEmpty()) {
            throw new IllegalStateException("OPENAI_API_KEY environment variable is required");
        }
        
        return OpenAiEmbeddingModel.builder()
                .apiKey(OPENAI_API_KEY)
                .modelName("text-embedding-3-small")  // This produces 1536 dimensions by default
                .dimensions(1024)  // Force it to use 1024 dimensions to match your Pinecone index
                .timeout(Duration.ofSeconds(60))
                .build();
    }
    
    public static PineconeEmbeddingStore createPineconeEmbeddingStore() {
        if (PINECONE_API_KEY == null || PINECONE_API_KEY.isEmpty()) {
            throw new IllegalStateException("PINECONE_API_KEY environment variable is required");
        }
        
        return PineconeEmbeddingStore.builder()
                .apiKey(PINECONE_API_KEY)
                .index(PINECONE_INDEX_NAME)
                .build();
    }
    
    // Vector Store Configuration Getters
    public static boolean isSkipExistingDocuments() {
        return SKIP_EXISTING_DOCUMENTS;
    }

}
