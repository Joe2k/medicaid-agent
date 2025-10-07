package org.usfca.medicaid;

import org.usfca.medicaid.chatbot.MedicaidChatbot;
import org.usfca.medicaid.service.DocumentLoaderService;
import org.usfca.medicaid.service.RagService;
import org.usfca.medicaid.service.VectorStoreService;

import java.util.List;
import java.util.Scanner;

/**
 * Main application controller for the Minnesota Medicaid Eligibility Chatbot.
 * Manages the application lifecycle, user interface, and coordinates services.
 */
public class MedicaidApplication {
    
    private Scanner scanner;
    private VectorStoreService vectorStoreService;
    private DocumentLoaderService documentLoaderService;
    private RagService ragService;
    private boolean running;
    
    /**
     * Initialize the application and its dependencies.
     */
    private void initialize() {
        System.out.println("=== Minnesota Medicaid Eligibility Chatbot ===");
        scanner = new Scanner(System.in);
        vectorStoreService = new VectorStoreService();
        documentLoaderService = new DocumentLoaderService();
        ragService = new RagService(vectorStoreService);
        running = true;
    }
    
    /**
     * Clean up resources before application exit.
     */
    private void cleanup() {
        if (scanner != null) {
            scanner.close();
        }
    }
    
    /**
     * Run the application main loop.
     */
    public void run() {
        try {
            initialize();
            
            while (running) {
                displayMainMenu();
                String choice = scanner.nextLine().trim();
                
                handleMenuChoice(choice);
                
                if (running && !choice.equals("2")) {
                    System.out.println("\nPress Enter to continue...");
                    scanner.nextLine();
                }
            }
            
            cleanup();
            
        } catch (Exception e) {
            System.err.println("Error running the application: " + e.getMessage());
            System.err.println("Please check your environment variables:");
            System.err.println("- OPENAI_API_KEY");
            System.err.println("- PINECONE_API_KEY");
            System.err.println("- PINECONE_ENVIRONMENT (optional, defaults to us-east-1-aws)");
            System.err.println("- PINECONE_INDEX_NAME (optional, defaults to medicaid-v1)");
            e.printStackTrace();
        }
    }
    
    /**
     * Handle user menu selection.
     *
     * @param choice the user's menu choice
     */
    private void handleMenuChoice(String choice) {
        switch (choice) {
            case "1":
                loadDocuments();
                break;
            case "2":
                startChat();
                break;
            case "3":
                exitApplication();
                break;
            default:
                System.out.println("\n❌ Invalid choice. Please enter 1, 2, or 3.");
                break;
        }
    }
    
    /**
     * Display the main menu.
     */
    private void displayMainMenu() {
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
     * Load documents into the vector store.
     */
    private void loadDocuments() {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("Loading Documents...");
        System.out.println("=".repeat(60));
        
        try {
            System.out.println("🗑️  Clearing existing documents from vector store...");
            vectorStoreService.clearAllDocuments();
            System.out.println("✅ Vector store cleared.\n");
            
            System.out.println("Loading documents from configured sources...");
            
            List<dev.langchain4j.data.document.Document> documents = documentLoaderService.loadDocuments();
            
            System.out.println("\nLoading " + documents.size() + " documents into the knowledge base...");
            
            vectorStoreService.addDocuments(documents);
            
            System.out.println("\n✅ Documents loaded successfully!");
            
        } catch (Exception e) {
            System.err.println("\n❌ Error loading documents: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Start the chatbot conversation.
     */
    private void startChat() {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("Starting Chat...");
        System.out.println("=".repeat(60));
        
        try {
            System.out.println("Starting the chatbot...\n");
            MedicaidChatbot chatbot = new MedicaidChatbot(scanner, ragService);
            chatbot.start();
            
        } catch (Exception e) {
            System.err.println("\n❌ Error starting chatbot: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Exit the application.
     */
    private void exitApplication() {
        System.out.println("\nThank you for using the Minnesota Medicaid Assistant. Goodbye!");
        running = false;
    }
}

