package org.usfca.medicaid.service;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatModel;
import org.usfca.medicaid.config.AppConfig;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service that implements Retrieval-Augmented Generation (RAG) for the chatbot.
 */
public class RagService {

    private final VectorStoreService vectorStoreService;
    private final ChatModel chatModel;

    /**
     * Constructs a new RagService with the provided vector store service.
     *
     * @param vectorStoreService the vector store service for document retrieval
     */
    public RagService(VectorStoreService vectorStoreService) {
        this.vectorStoreService = vectorStoreService;
        this.chatModel = AppConfig.createChatModel();
    }

    /**
     * Generate a response using RAG with conversation history for context-aware responses.
     * Retrieves relevant documents and uses them as context for the language model.
     *
     * @param userQuery the user's question or query
     * @param conversationHistory the previous conversation messages for context
     * @return a generated response based on the retrieved context and conversation history
     */
    public String generateResponse(String userQuery, List<String> conversationHistory) {
        log("------------------------------------------------------------");
        log(String.format("🤔 Thinking... received question: \"%s\"", userQuery));

        List<TextSegment> relevantDocuments = retrieveRelevantDocuments(userQuery, conversationHistory);

        if (relevantDocuments.isEmpty()) {
            log("⚠️  No relevant documents found. Returning fallback message.");
            return "I'm sorry, I couldn't find relevant information about your query in the Minnesota Medicaid documentation. " +
                    "Please try rephrasing your question or contact the Minnesota Department of Human Services for assistance.";
        }

        String context = buildContextFromDocuments(relevantDocuments);

        log(String.format("📚 Compiled %d document segments into context.", relevantDocuments.size()));

        String prompt = buildPromptWithHistory(context, userQuery, conversationHistory);

        log("💬 Calling language model for final response...");
        String answer = chatModel.chat(prompt);
        log("✅ Language model returned an answer.");
        return answer;
    }

    /**
     * Build context string from retrieved documents.
     *
     * @param documents the list of retrieved document segments
     * @return a formatted string containing all document text separated by dividers
     */
    private String buildContextFromDocuments(List<TextSegment> documents) {
        return documents.stream()
                .map(TextSegment::text)
                .collect(Collectors.joining("\n\n---\n\n"));
    }

    /**
     * Build the prompt for the chat model with context and instructions.
     *
     * @param context the context from retrieved documents
     * @param userQuery the user's question
     * @return a formatted prompt for the language model
     */
    private String buildPrompt(String context, String userQuery) {
        return String.format("""
            You are a helpful assistant specializing in Minnesota Medicaid eligibility and benefits information. 
            Use the following context from official Minnesota Medicaid documentation to answer the user's question.
            
            Context:
            %s
            
            User Question: %s
            
            Instructions:
            1. Answer based ONLY on the provided context
            2. If the context doesn't contain enough information to fully answer the question, say so
            3. Be specific about eligibility requirements, benefits, and processes
            4. Include relevant contact information or next steps when appropriate
            5. If discussing income limits, mention that they are subject to change and should be verified
            6. Always remind users that this is general information and they should contact the Minnesota Department of Human Services for their specific situation
            
            Answer:""", context, userQuery);
    }

    /**
     * Build the prompt with conversation history for context-aware responses.
     *
     * @param context the context from retrieved documents
     * @param userQuery the user's question
     * @param conversationHistory the previous conversation messages
     * @return a formatted prompt for the language model
     */
    private String buildPromptWithHistory(String context, String userQuery, List<String> conversationHistory) {
        if (conversationHistory == null || conversationHistory.isEmpty()) {
            return buildPrompt(context, userQuery);
        }

        String historyText = getRecentConversationHistory(conversationHistory);

        return String.format("""
            You are a helpful assistant specializing in Minnesota Medicaid eligibility and benefits information. 
            Use the following context from official Minnesota Medicaid documentation to answer the user's question.
            
            Context:
            %s
            
            Previous Conversation:
            %s
            
            Current User Question: %s
            
            Instructions:
            1. Answer based ONLY on the provided context
            2. Consider the conversation history to understand follow-up questions
            3. If the question refers to something mentioned earlier (like "Is there a specific program"), use the conversation history to understand what they're asking about
            4. Be specific about eligibility requirements, benefits, and processes
            5. Include relevant contact information or next steps when appropriate
            6. If discussing income limits, mention that they are subject to change and should be verified
            7. Always remind users that this is general information and they should contact the Minnesota Department of Human Services for their specific situation
            
            Answer:""", context, historyText, userQuery);
    }

    /**
     * Retrieve relevant documents for the user's query after generating improved search queries.
     *
     * @param userQuery the user's original query
     * @param conversationHistory previous conversation messages
     * @return a list of relevant text segments
     */
    private List<TextSegment> retrieveRelevantDocuments(String userQuery, List<String> conversationHistory) {
        List<String> searchQueries = generateSearchQueries(userQuery, conversationHistory);

        Map<String, TextSegment> uniqueMatches = new LinkedHashMap<>();

        for (String query : searchQueries) {
            log(String.format("🔎 Searching Pinecone with query: \"%s\"", query));
            try {
                List<TextSegment> matches = vectorStoreService.searchRelevantDocuments(query, 5, 0.65);
                log(String.format("   ↳ Retrieved %d matches.", matches.size()));
                for (TextSegment match : matches) {
                    String key = buildSegmentKey(match);
                    uniqueMatches.putIfAbsent(key, match);
                }
            } catch (Exception ex) {
                log(String.format("⚠️ Vector search failed for query '%s': %s", query, ex.getMessage()));
            }
        }

        log(String.format("📦 Total unique segments accumulated: %d", uniqueMatches.size()));

        return new ArrayList<>(uniqueMatches.values());
    }

    /**
     * Generate a set of optimized search queries using a query rewriting prompt.
     *
     * @param userQuery the original user query
     * @param conversationHistory the recent conversation history for context
     * @return a list of unique search queries to execute against the vector store
     */
    private List<String> generateSearchQueries(String userQuery, List<String> conversationHistory) {
        List<String> queries = new ArrayList<>();
        queries.add(userQuery);

        String historyText = getRecentConversationHistory(conversationHistory);

        log("🛠️  Generating rewritten search queries...");

        String rewritePrompt = String.format("""
            You are assisting with retrieval for a Minnesota Medicaid knowledge base.
            Generate up to three alternative semantic search queries that would help retrieve context for answering the user.
            Focus on transforming follow-up questions into standalone queries and expand acronyms where appropriate.

            Original question: %s

            Recent conversation context:
            %s

            Output Format:
            - Return only the new search queries, one per line.
            - Do not number the queries or add explanations.
            - If the original question is already clear, return a single identical line.
            """,
                userQuery,
                historyText.isEmpty() ? "None provided." : historyText
        );

        try {
            String rewriteResponse = chatModel.chat(rewritePrompt);
            List<String> rewrittenQueries = parseRewrittenQueries(rewriteResponse);

            for (String rewritten : rewrittenQueries) {
                if (!rewritten.equalsIgnoreCase(userQuery) && !queries.contains(rewritten)) {
                    queries.add(rewritten);
                }
            }

            log(String.format("   ↳ Generated %d rewritten queries.", queries.size() - 1));
            rewrittenQueries.forEach(q -> log(String.format("      • %s", q)));
        } catch (Exception ex) {
            log(String.format("⚠️ Query rewriting failed: %s", ex.getMessage()));
        }

        if (queries.size() == 1) {
            log("   ↳ No alternative queries generated; using original question only.");
        }

        return queries;
    }

    /**
     * Parse the rewritten queries response into individual search queries.
     *
     * @param response the raw response text from the language model
     * @return a list of parsed queries
     */
    private List<String> parseRewrittenQueries(String response) {
        if (response == null || response.isBlank()) {
            return List.of();
        }

        return response.lines()
                .map(String::trim)
                .filter(line -> !line.isEmpty())
                .map(line -> line.replaceAll("^(\\d+\\.|[-*•])\\s*", ""))
                .distinct()
                .limit(3)
                .toList();
    }

    /**
     * Build a unique key for text segments to avoid duplicates in the context.
     *
     * @param segment the text segment returned from the vector search
     * @return a stable key representing the segment
     */
    private String buildSegmentKey(TextSegment segment) {
        if (segment == null) {
            return "null-segment";
        }

        Map<String, Object> metadataMap = segment.metadata() != null ? segment.metadata().toMap() : Map.of();
        String documentId = String.valueOf(metadataMap.getOrDefault("document_id", metadataMap.getOrDefault("source", "unknown_document")));
        String segmentIndex = String.valueOf(metadataMap.getOrDefault("segment_index", segment.text().hashCode()));

        return documentId + "::" + segmentIndex;
    }

    /**
     * Extract recent conversation history for prompts.
     *
     * @param conversationHistory the full conversation history
     * @return a string with the most recent exchanges
     */
    private String getRecentConversationHistory(List<String> conversationHistory) {
        if (conversationHistory == null || conversationHistory.isEmpty()) {
            return "";
        }

        int startIndex = Math.max(0, conversationHistory.size() - 6);
        List<String> recentHistory = conversationHistory.subList(startIndex, conversationHistory.size());

        return String.join("\n", recentHistory);
    }

    private void log(String message) {
        if (AppConfig.isDebugLoggingEnabled()) {
            System.out.println("[DEBUG] " + message);
        }
    }
}
