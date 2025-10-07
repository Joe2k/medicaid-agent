package org.usfca.medicaid;

import org.usfca.medicaid.chatbot.MedicaidChatbot;
import org.usfca.medicaid.service.DocumentLoaderService;
import org.usfca.medicaid.service.RagService;

import java.util.List;
import java.util.Scanner;

/**
 * Main entry point for the Minnesota Medicaid Eligibility Chatbot application.
 */
public class Main {
    private static Scanner scanner;
    
    /**
     * Main method that starts the application.
     * Displays a menu allowing users to load documents or start chatting with the agent.
     *
     * @param args command line arguments (not used)
     */
    public static void main(String[] args) {
        System.out.println("=== Minnesota Medicaid Eligibility Chatbot ===");
        
        try {
            scanner = new Scanner(System.in);
            boolean running = true;
            
            while (running) {
                displayMainMenu();
                String choice = scanner.nextLine().trim();
                
                switch (choice) {
                    case "1":
                        loadDocuments();
                        break;
                    case "2":
                        startChat();
                        break;
                    case "3":
                        System.out.println("\nThank you for using the Minnesota Medicaid Assistant. Goodbye!");
                        running = false;
                        break;
                    default:
                        System.out.println("\n❌ Invalid choice. Please enter 1, 2, or 3.");
                        break;
                }
                
                if (running && !choice.equals("2")) {
                    System.out.println("\nPress Enter to continue...");
                    scanner.nextLine();
                }
            }
            
            scanner.close();
            
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
    
    /**
     * Display the main menu
     */
    private static void displayMainMenu() {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("                    MAIN MENU");
        System.out.println("=".repeat(60));
        System.out.println("1. Load Documents into Vector Store");
        System.out.println("2. Start Chat with Agent");
        System.out.println("3. Exit");
        System.out.println("=".repeat(60));
        System.out.print("Enter your choice (1-3): ");
    }
    
    /**
     * Load documents into the vector store
     */
    private static void loadDocuments() {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("Loading Documents...");
        System.out.println("=".repeat(60));
        
        try {
            System.out.println("Initializing the RAG system...");
            RagService ragService = new RagService();
            
            DocumentLoaderService documentLoader = new DocumentLoaderService();
            List<dev.langchain4j.data.document.Document> documents = documentLoader.loadDocuments();
            
            System.out.println("\nLoading " + documents.size() + " documents into the knowledge base...");
            
            ragService.addDocuments(documents);
            
            System.out.println("\n✅ Documents loaded successfully!");
            
        } catch (Exception e) {
            System.err.println("\n❌ Error loading documents: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Start the chatbot
     */
    private static void startChat() {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("Starting Chat...");
        System.out.println("=".repeat(60));
        
        try {
            System.out.println("Initializing the chatbot...\n");
            
            MedicaidChatbot chatbot = new MedicaidChatbot(scanner);
            chatbot.start();
            
        } catch (Exception e) {
            System.err.println("\n❌ Error starting chatbot: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
