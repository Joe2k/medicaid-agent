package org.usfca.medicaid.chatbot;

import org.usfca.medicaid.service.RagService;

import java.util.Scanner;

public class MedicaidChatbot {
    
    private final RagService ragService;
    private final Scanner scanner;
    private boolean isRunning;

    public MedicaidChatbot(Scanner scanner) {
        this.ragService = new RagService();
        this.scanner = scanner;
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
                
                // Generate and display response
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
     * Check if the input is an exit command
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
     * Check if the chatbot is running
     */
    public boolean isRunning() {
        return isRunning;
    }
}
