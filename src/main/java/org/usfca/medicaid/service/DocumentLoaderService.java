package org.usfca.medicaid.service;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.Metadata;
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.usfca.medicaid.config.DocumentConfig;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Service for loading documents from various sources including URLs, PDFs,
 * text files, and other file types.
 */
public class DocumentLoaderService {
    
    private final Tika tika;
    private final Random random;
    private final Map<String, String> cookies;
    private final String[] userAgents;
    
    /**
     * Constructs a new DocumentLoaderService with an initialized Tika instance.
     */
    public DocumentLoaderService() {
        this.tika = new Tika();
        this.random = new Random();
        this.cookies = new HashMap<>();
        this.userAgents = new String[]{
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.1 Safari/605.1.15"
        };
    }

    /**
     * Load all documents from DocumentConfig.
     *
     * @return a list of loaded documents
     */
    public List<Document> loadDocuments() {
        List<String> sources = DocumentConfig.getDocumentSources();
        
        System.out.println("📚 Loading " + sources.size() + " documents from various sources...");
        
        List<Document> documents = new ArrayList<>();
        
        for (int i = 0; i < sources.size(); i++) {
            String source = sources.get(i);
            try {
                Document document = loadDocument(source);
                if (document != null) {
                    documents.add(document);
                    System.out.println("✅ Successfully loaded: " + source);
                }
            } catch (Exception e) {
                System.err.println("❌ Error loading " + source + ": " + e.getMessage());
            }
        }
        
        return documents;
    }
    
    /**
     * Load a single document from various sources.
     * Automatically detects the source type and uses the appropriate loader.
     *
     * @param source the source path or URL
     * @return the loaded document
     * @throws IOException if an I/O error occurs
     * @throws TikaException if document parsing fails
     */
    public Document loadDocument(String source) throws IOException, TikaException {
        if (source.startsWith("http://") || source.startsWith("https://")) {
            return loadFromUrl(source);
        } else if (source.toLowerCase().endsWith(".pdf")) {
            return loadFromPdfFile(source);
        } else if (source.toLowerCase().endsWith(".txt")) {
            return loadFromTextFile(source);
        } else {
            return loadFromFile(source);
        }
    }
    
    /**
     * Load content from a URL using JSoup.
     *
     * @param url the URL to load content from
     * @return a document containing the web page content
     * @throws IOException if the URL cannot be accessed or parsed
     */
    private Document loadFromUrl(String url) throws IOException {
        System.out.println("🌐 Loading content from URL: " + url);

        String randomUserAgent = userAgents[random.nextInt(userAgents.length)];

        org.jsoup.Connection.Response response = Jsoup.connect(url)
                .userAgent(randomUserAgent)
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7")
                .header("Accept-Language", "en-US,en;q=0.9")
                .header("Accept-Encoding", "gzip, deflate, br")
                .header("Connection", "keep-alive")
                .header("Upgrade-Insecure-Requests", "1")
                .header("Sec-Fetch-Dest", "document")
                .header("Sec-Fetch-Mode", "navigate")
                .header("Sec-Fetch-Site", "none")
                .header("Sec-Fetch-User", "?1")
                .header("Cache-Control", "max-age=0")
                .header("DNT", "1")
                .referrer("https://www.google.com/")
                .cookies(cookies)
                .timeout(20000)
                .followRedirects(true)
                .maxBodySize(0)
                .execute();

        cookies.putAll(response.cookies());
        
        org.jsoup.nodes.Document doc = response.parse();

        String title = doc.title();
        if (title == null || title.isEmpty()) {
            title = "Web Content from " + url;
        }

        if (isBotProtectionPage(doc, title)) {
            System.out.println("⚠️  Bot protection detected, skipping: " + url);
            throw new IOException("Bot protection page detected - cannot load content");
        }

        doc.select("script, style").remove();

        StringBuilder content = new StringBuilder();

        Elements mainContent = doc.select("main, article, .content, .main-content, #content, #main");
        if (!mainContent.isEmpty()) {
            content.append(mainContent.text());
        } else {
            Element body = doc.body();
            if (body != null) {
                content.append(body.text());
            }
        }
        
        String cleanContent = content.toString()
                .replaceAll("\\s+", " ")
                .trim();
        
        if (cleanContent.isEmpty()) {
            throw new IOException("No content found on the webpage");
        }
        
        return createDocument(title, cleanContent, "web", "url", url);
    }
    
    /**
     * Check if the page is a bot protection page.
     *
     * @param doc the JSoup document
     * @param title the page title
     * @return true if bot protection is detected, false otherwise
     */
    private boolean isBotProtectionPage(org.jsoup.nodes.Document doc, String title) {
        String lowerTitle = title.toLowerCase();
        String bodyText = doc.body() != null ? doc.body().text().toLowerCase() : "";

        if (lowerTitle.contains("radware") || lowerTitle.contains("captcha")) {
            return true;
        }

        if (bodyText.length() < 500) {
            return true;
        }

        String[] botIndicators = {"bot manager", "access denied", "blocked", "security check"};
        for (String indicator : botIndicators) {
            if (lowerTitle.contains(indicator) || bodyText.contains(indicator)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Load content from a PDF file using Apache Tika.
     *
     * @param filePath the path to the PDF file
     * @return a document containing the PDF content
     * @throws IOException if the file cannot be read
     * @throws TikaException if PDF parsing fails
     */
    private Document loadFromPdfFile(String filePath) throws IOException, TikaException {
        System.out.println("📄 Loading PDF file: " + filePath);
        
        Path path = Paths.get(filePath);
        if (!Files.exists(path)) {
            throw new IOException("PDF file does not exist: " + filePath);
        }
        
        try (InputStream inputStream = new FileInputStream(filePath)) {
            String content = tika.parseToString(inputStream);
            
            if (content == null || content.trim().isEmpty()) {
                throw new IOException("No content extracted from PDF");
            }
            
            String cleanContent = content
                    .replaceAll("\\s+", " ")
                    .trim();
            
            String fileName = path.getFileName().toString();
            String title = fileName.replace(".pdf", "");
            
            return createDocument(title, cleanContent, "pdf", "file", filePath);
        }
    }
    
    /**
     * Load content from a text file.
     *
     * @param filePath the path to the text file
     * @return a document containing the text file content
     * @throws IOException if the file cannot be read
     */
    private Document loadFromTextFile(String filePath) throws IOException {
        System.out.println("📝 Loading text file: " + filePath);
        
        Path path = Paths.get(filePath);
        if (!Files.exists(path)) {
            throw new IOException("Text file does not exist: " + filePath);
        }
        
        String content = Files.readString(path);
        String fileName = path.getFileName().toString();
        String title = fileName.replace(".txt", "");
        
        return createDocument(title, content, "text", "file", filePath);
    }
    
    /**
     * Load content from any file with automatic type detection using Apache Tika.
     *
     * @param filePath the path to the file
     * @return a document containing the file content
     * @throws IOException if the file cannot be read
     * @throws TikaException if content extraction fails
     */
    private Document loadFromFile(String filePath) throws IOException, TikaException {
        System.out.println("📁 Loading file (auto-detect): " + filePath);
        
        Path path = Paths.get(filePath);
        if (!Files.exists(path)) {
            throw new IOException("File does not exist: " + filePath);
        }
        
        try (InputStream inputStream = new FileInputStream(filePath)) {
            String content = tika.parseToString(inputStream);
            
            if (content == null || content.trim().isEmpty()) {
                throw new IOException("No content extracted from file");
            }
            
            String cleanContent = content
                    .replaceAll("\\s+", " ")
                    .trim();
            
            String fileName = path.getFileName().toString();
            String fileType = tika.detect(path.toFile());
            
            return createDocument(fileName, cleanContent, fileType, "file", filePath);
        }
    }
    
    /**
     * Create a document with metadata.
     *
     * @param title the document title
     * @param content the document content
     * @param category the document category (e.g., "web", "pdf", "text")
     * @param type the document type (e.g., "url", "file")
     * @param source the source path or URL
     * @return a document with the specified content and metadata
     */
    private Document createDocument(String title, String content, String category, String type, String source) {
        Metadata metadata = new Metadata();
        metadata.put("title", title);
        metadata.put("category", category);
        metadata.put("type", type);
        metadata.put("source", source);
        metadata.put("loaded_at", java.time.Instant.now().toString());
        
        return Document.from(content, metadata);
    }
}
