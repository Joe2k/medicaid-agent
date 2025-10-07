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
                "https://mn.gov/dhs/people-we-serve/adults/health-care/health-care-programs/programs-and-services/",
                "https://mn.gov/dhs/people-we-serve/adults/health-care/health-care-programs/programs-and-services/medical-assistance.jsp",
                "https://mn.gov/dhs/people-we-serve/adults/health-care/health-care-programs/programs-and-services/income-asset-limits.jsp",
                "https://mn.gov/dhs/people-we-serve/children-and-families/health-care/health-care-programs/programs-and-services/ma-coverage.jsp",
                "https://mn.gov/dhs/people-we-serve/children-and-families/health-care/health-care-programs/programs-and-services/other-insurance.jsp",
                "https://www.mnsure.org/financial-help/ma-mncare/",
                "https://www.mnsure.org/financial-help/income-guidelines/index.jsp",
                "https://mn.gov/dhs/people-we-serve/adults/health-care/health-care-programs/programs-and-services/ema.jsp",
                "https://mn.gov/dhs/people-we-serve/adults/health-care/health-care-programs/programs-and-services/ma-waiver-programs.jsp",
                "https://mn.gov/dhs/people-we-serve/adults/health-care/health-care-programs/programs-and-services/estate-recovery.jsp",
                "https://www.mnsure.org/shop-compare/right-plan/drug-lists.jsp",
                "https://mn.gov/dhs/assets/preferred-drug-list-2025-01-01_tcm1053-662387.pdf"
        );
    }
}
