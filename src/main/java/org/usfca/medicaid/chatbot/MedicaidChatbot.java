package org.usfca.medicaid.chatbot;

import org.usfca.medicaid.service.RagService;

import java.util.Scanner;

/**
 * Interactive chatbot for answering Minnesota Medicaid eligibility questions.
 */
public class MedicaidChatbot {
    
    private final RagService ragService;
    private final Scanner scanner;
    private boolean isRunning;

    /**
     * Constructs a new MedicaidChatbot with the specified scanner and RAG service.
     *
     * @param scanner the scanner to use for reading user input
     * @param ragService the RAG service for generating responses
     */
    public MedicaidChatbot(Scanner scanner, RagService ragService) {
        this.scanner = scanner;
        this.ragService = ragService;
        this.isRunning = false;
    }
    
    /**
     * Start the chatbot conversation
     */
    public void start() {
        isRunning = true;
        displayWelcomeMessage();
        
        while (isRunning) {
            try {
                System.out.print("\nYou: ");
                String userInput = scanner.nextLine().trim();
                
                if (userInput.isEmpty()) {
                    continue;
                }
                
                if (isExitCommand(userInput)) {
                    handleExitCommand();
                    break;
                }
                
                System.out.print("Medicaid Assistant: ");
                String response = ragService.generateResponse(userInput);
                System.out.println(response);
                
            } catch (Exception e) {
                System.err.println("An error occurred: " + e.getMessage());
                System.out.println("Please try again.");
            }
        }
        
        System.out.println("\nReturning to main menu...");
    }
    
    /**
     * Display welcome message
     */
    private void displayWelcomeMessage() {
        System.out.println("=".repeat(60));
        System.out.println("🤖 Welcome to the Medicaid Chatbot Assistant! 🤖");
        System.out.println("=".repeat(60));
        System.out.println("I can help you with questions about your documents.");
        System.out.println("Ask me anything about the content in your knowledge base!");
        System.out.println("\nType 'exit' to quit.");
        System.out.println("=".repeat(60));
    }
    
    /**
     * Check if the input is an exit command.
     *
     * @param input the user input to check
     * @return true if the input is an exit command, false otherwise
     */
    private boolean isExitCommand(String input) {
        String lowerInput = input.toLowerCase();
        return lowerInput.equals("exit") || 
               lowerInput.equals("quit") || 
               lowerInput.equals("bye") || 
               lowerInput.equals("goodbye");
    }
    
    /**
     * Handle exit command
     */
    private void handleExitCommand() {
        isRunning = false;
    }
    
    /**
     * Stop the chatbot
     */
    public void stop() {
        isRunning = false;
    }
    
    /**
     * Check if the chatbot is running.
     *
     * @return true if the chatbot is running, false otherwise
     */
    public boolean isRunning() {
        return isRunning;
    }
}
