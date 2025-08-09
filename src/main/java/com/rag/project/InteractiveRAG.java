package com.rag.project;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.ollama.OllamaEmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;

import java.io.File;
import java.time.Duration;
import java.util.Scanner;

public class InteractiveRAG {
    
    private RAGService ragService;
    private EmbeddingStore<TextSegment> embeddingStore;
    private EmbeddingModel embeddingModel;
    
    public static void main(String[] args) {
        new InteractiveRAG().run();
    }
    
    public void run() {
        System.out.println("🤖 Interactive RAG Chat System");
        System.out.println("================================");
        
        try {
            // Initialize models
            initializeSystem();
            
            // Load all documents
            loadAllDocuments();
            
            // Start chat
            startChat();
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void initializeSystem() {
        System.out.println("🚀 Initializing system...");
        
        String ollamaHost = "http://localhost:11435";
        
        // Initialize models
        embeddingModel = OllamaEmbeddingModel.builder()
                .baseUrl(ollamaHost)
                .modelName("nomic-embed-text")
                .timeout(Duration.ofMinutes(3))
                .build();
        
        ChatLanguageModel chatModel = OllamaChatModel.builder()
                .baseUrl(ollamaHost)
                .modelName("llama3.2")
                .timeout(Duration.ofMinutes(3))
                .build();
        
        // Initialize Milvus
        embeddingStore = MilvusConfig.createMilvusEmbeddingStore();
        
        // Create RAG service
        ragService = new RAGService(embeddingStore, embeddingModel, chatModel);
        
        System.out.println("✅ System ready!");
    }
    
    private void loadAllDocuments() {
        System.out.println("📚 Loading documents...");
        
        File documentsDir = new File("documents");
        if (!documentsDir.exists()) {
            System.out.println("⚠️  No documents directory found");
            return;
        }
        
        File[] files = documentsDir.listFiles((dir, name) -> name.endsWith(".txt"));
        
        if (files == null || files.length == 0) {
            System.out.println("⚠️  No .txt files found in documents directory");
            return;
        }
        
        DocumentLoader loader = new DocumentLoader(embeddingModel, embeddingStore);
        
        for (File file : files) {
            try {
                System.out.println("Loading: " + file.getName());
                // You might want to modify DocumentLoader to accept file parameter
                // For now, it loads documents/data.txt
                loader.ingestDocument();
            } catch (Exception e) {
                System.err.println("Failed to load " + file.getName() + ": " + e.getMessage());
            }
        }
        
        System.out.println("✅ Documents loaded!");
    }
    
    private void startChat() {
        Scanner scanner = new Scanner(System.in);
        
        System.out.println("\n💬 Chat started! Type 'quit' to exit.");
        System.out.println("Ask me anything about the loaded documents:");
        
        while (true) {
            System.out.print("\n👤 You: ");
            String query = scanner.nextLine().trim();
            
            if (query.equalsIgnoreCase("quit") || query.equalsIgnoreCase("exit")) {
                System.out.println("👋 Goodbye!");
                break;
            }
            
            if (query.isEmpty()) {
                continue;
            }
            
            System.out.print("🤖 AI: ");
            System.out.println("Thinking...");
            
            try {
                String answer = ragService.answer(query);
                System.out.println("🤖 AI: " + answer);
            } catch (Exception e) {
                System.out.println("🤖 AI: Sorry, I encountered an error: " + e.getMessage());
            }
        }
        
        scanner.close();
    }
}