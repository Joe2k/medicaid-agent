package org.usfca.medicaid.config;

import java.util.Arrays;
import java.util.List;

public class DocumentConfig {
    
    /**
     * Get all document sources for the RAG system
     * Add your PDF files, URLs, and text files here
     */
    public static List<String> getDocumentSources() {
        return Arrays.asList(
                "https://www.kff.org/medicaid/health-policy-101-medicaid/?entry=table-of-contents-what-long-term-services-and-supports-ltss-are-covered-by-medicaid",
                "https://mn.gov/dhs/people-we-serve/adults/health-care/health-care-programs/programs-and-services/"
        );
    }
}
