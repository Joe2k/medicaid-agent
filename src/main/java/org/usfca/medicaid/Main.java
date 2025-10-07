package org.usfca.medicaid;

/**
 * Main entry point for the Minnesota Medicaid Eligibility Chatbot application.
 */
public class Main {
    
    /**
     * Main method that starts the application.
     *
     * @param args command line arguments (not used)
     */
    public static void main(String[] args) {
        MedicaidApplication app = new MedicaidApplication();
        app.run();
    }
}
