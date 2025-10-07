package org.usfca.medicaid.config;

import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.store.embedding.pinecone.PineconeEmbeddingStore;

import java.time.Duration;

public class AppConfig {

    private static final String OPENAI_API_KEY = System.getenv("OPENAI_API_KEY");
    private static final String PINECONE_API_KEY = System.getenv("PINECONE_API_KEY");
    private static final String PINECONE_ENVIRONMENT = System.getenv().getOrDefault("PINECONE_ENVIRONMENT", "us-east-1-aws");
    private static final String PINECONE_INDEX_NAME = System.getenv().getOrDefault("PINECONE_INDEX_NAME", "medicaid-v1");
    
    private static final String openaiChatModel = "gpt-3.5-turbo";
    private static final String openaiEmbeddingModel = "text-embedding-3-small";
    
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
                .modelName(openaiChatModel)
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
                .modelName(openaiEmbeddingModel)
                .dimensions(1024)
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
    
    public static boolean isSkipExistingDocuments() {
        return SKIP_EXISTING_DOCUMENTS;
    }

}
