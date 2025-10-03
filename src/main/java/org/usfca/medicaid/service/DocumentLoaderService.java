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
import java.util.List;

public class DocumentLoaderService {
    
    private final Tika tika;
    
    public DocumentLoaderService() {
        this.tika = new Tika();
    }

    /**
     * Load all documents from DocumentConfig
     */
    public List<Document> loadDocuments() {
        List<String> sources = DocumentConfig.getDocumentSources();
        
        System.out.println("📚 Loading " + sources.size() + " documents from various sources...");
        
        List<Document> documents = new ArrayList<>();
        
        for (String source : sources) {
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
     * Load a single document from various sources
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
     * Load content from a URL
     */
    private Document loadFromUrl(String url) throws IOException {
        System.out.println("🌐 Loading content from URL: " + url);

        org.jsoup.nodes.Document doc = Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .timeout(10000)
                .get();

        String title = doc.title();
        if (title == null || title.isEmpty()) {
            title = "Web Content from " + url;
        }

        doc.select("script, style").remove();

        StringBuilder content = new StringBuilder();

        Elements mainContent = doc.select("main, article, .content, .main-content, #content, #main");
        if (!mainContent.isEmpty()) {
            content.append(mainContent.text());
        } else {
            // Fallback to body content
            Element body = doc.body();
            if (body != null) {
                content.append(body.text());
            }
        }
        
        // Clean up the content
        String cleanContent = content.toString()
                .replaceAll("\\s+", " ")  // Replace multiple whitespace with single space
                .trim();
        
        if (cleanContent.isEmpty()) {
            throw new IOException("No content found on the webpage");
        }
        
        return createDocument(title, cleanContent, "web", "url", url);
    }
    
    /**
     * Load content from a PDF file
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
            
            // Clean up the content
            String cleanContent = content
                    .replaceAll("\\s+", " ")  // Replace multiple whitespace with single space
                    .trim();
            
            String fileName = path.getFileName().toString();
            String title = fileName.replace(".pdf", "");
            
            return createDocument(title, cleanContent, "pdf", "file", filePath);
        }
    }
    
    /**
     * Load content from a text file
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
     * Load content from any file (auto-detect type)
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
            
            // Clean up the content
            String cleanContent = content
                    .replaceAll("\\s+", " ")  // Replace multiple whitespace with single space
                    .trim();
            
            String fileName = path.getFileName().toString();
            String fileType = tika.detect(path.toFile());
            
            return createDocument(fileName, cleanContent, fileType, "file", filePath);
        }
    }
    
    /**
     * Create a document with metadata
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
