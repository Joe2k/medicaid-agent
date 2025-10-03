package org.usfca.medicaid;

import org.usfca.medicaid.chatbot.MedicaidChatbot;
import org.usfca.medicaid.service.DocumentLoaderService;
import org.usfca.medicaid.service.RagService;

import java.util.List;

public class Main {
    public static void main(String[] args) {
        System.out.println("=== Minnesota Medicaid Eligibility Chatbot ===");
        System.out.println("Initializing the RAG system...");
        
        try {
            // Initialize the RAG service
            RagService ragService = new RagService();
            
            // Load your own documents
            DocumentLoaderService documentLoader = new DocumentLoaderService();
            List<dev.langchain4j.data.document.Document> documents = documentLoader.loadDocuments();
            
            System.out.println("Loading " + documents.size() + " documents into the knowledge base...");
            
            // Add documents to the vector store
            ragService.addDocuments(documents);
            
            System.out.println("Documents loaded successfully!");
            System.out.println("Starting the chatbot...\n");
            
            // Start the chatbot
            MedicaidChatbot chatbot = new MedicaidChatbot();
            chatbot.start();
            
        } catch (Exception e) {
            System.err.println("Error initializing the chatbot: " + e.getMessage());
            System.err.println("Please check your environment variables:");
            System.err.println("- OPENAI_API_KEY");
            System.err.println("- PINECONE_API_KEY");
            System.err.println("- PINECONE_ENVIRONMENT (optional, defaults to us-east-1-aws)");
            System.err.println("- PINECONE_INDEX_NAME (optional, defaults to medicaid-minnesota)");
            e.printStackTrace();
        }
    }
}
