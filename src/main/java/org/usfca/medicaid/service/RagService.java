package org.usfca.medicaid.service;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.usfca.medicaid.config.AppConfig;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service that implements Retrieval-Augmented Generation (RAG) for the chatbot.
 */
public class RagService {
    
    private final VectorStoreService vectorStoreService;
    private final ChatLanguageModel chatModel;
    
    /**
     * Constructs a new RagService with initialized vector store and chat model.
     */
    public RagService() {
        this.vectorStoreService = new VectorStoreService();
        this.chatModel = AppConfig.createChatModel();
    }
    
    /**
     * Generate a response using RAG (Retrieval-Augmented Generation).
     * Retrieves relevant documents and uses them as context for the language model.
     *
     * @param userQuery the user's question or query
     * @return a generated response based on the retrieved context
     */
    public String generateResponse(String userQuery) {
        List<TextSegment> relevantDocuments = vectorStoreService.searchRelevantDocuments(userQuery, 5, 0.7);
        
        if (relevantDocuments.isEmpty()) {
            return "I'm sorry, I couldn't find relevant information about your query in the Minnesota Medicaid documentation. " +
                   "Please try rephrasing your question or contact the Minnesota Department of Human Services for assistance.";
        }
        
        String context = buildContextFromDocuments(relevantDocuments);
        
        String prompt = buildPrompt(context, userQuery);
        
        return chatModel.generate(prompt);
    }
    
    /**
     * Build context string from retrieved documents.
     *
     * @param documents the list of retrieved document segments
     * @return a formatted string containing all document text separated by dividers
     */
    private String buildContextFromDocuments(List<TextSegment> documents) {
        return documents.stream()
                .map(TextSegment::text)
                .collect(Collectors.joining("\n\n---\n\n"));
    }
    
    /**
     * Build the prompt for the chat model with context and instructions.
     *
     * @param context the context from retrieved documents
     * @param userQuery the user's question
     * @return a formatted prompt for the language model
     */
    private String buildPrompt(String context, String userQuery) {
        return String.format("""
            You are a helpful assistant specializing in Minnesota Medicaid eligibility and benefits information. 
            Use the following context from official Minnesota Medicaid documentation to answer the user's question.
            
            Context:
            %s
            
            User Question: %s
            
            Instructions:
            1. Answer based ONLY on the provided context
            2. If the context doesn't contain enough information to fully answer the question, say so
            3. Be specific about eligibility requirements, benefits, and processes
            4. Include relevant contact information or next steps when appropriate
            5. If discussing income limits, mention that they are subject to change and should be verified
            6. Always remind users that this is general information and they should contact the Minnesota Department of Human Services for their specific situation
            
            Answer:""", context, userQuery);
    }
    
    /**
     * Add documents to the knowledge base.
     *
     * @param documents the list of documents to add to the vector store
     */
    public void addDocuments(List<dev.langchain4j.data.document.Document> documents) {
        vectorStoreService.addDocuments(documents);
    }
}
