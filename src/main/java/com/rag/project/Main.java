package com.rag.project;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.ollama.OllamaEmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;

import java.time.Duration;

public class Main {
    public static void main(String[] args) {
        
        System.out.println("Starting RAG-Milvus application...");
        
        try {
            // Configuration - using port 11435
            String ollamaHost = "http://localhost:11435";
            
            // Initialize Embedding Model
            System.out.println("Initializing Ollama Embedding Model...");
            EmbeddingModel embeddingModel = OllamaEmbeddingModel.builder()
                    .baseUrl(ollamaHost)
                    .modelName("nomic-embed-text")
                    .timeout(Duration.ofMinutes(3))
                    .build();
            
            // Test embedding model first
            System.out.println("Testing embedding model...");
            try {
                embeddingModel.embed("test").content();
                System.out.println("✓ Embedding model is working!");
            } catch (Exception e) {
                System.err.println("✗ Embedding model failed: " + e.getMessage());
                System.err.println("Make sure Ollama is running and nomic-embed-text model is installed");
                return;
            }
            
            // Initialize Chat Model
            System.out.println("Initializing Ollama Chat Model...");
            ChatLanguageModel chatLanguageModel = OllamaChatModel.builder()
                    .baseUrl(ollamaHost)
                    .modelName("llama3.2")
                    .timeout(Duration.ofMinutes(3))
                    .build();
            
            // Test chat model
            System.out.println("Testing chat model...");
            try {
                chatLanguageModel.generate("Hello");
                System.out.println("✓ Chat model is working!");
            } catch (Exception e) {
                System.err.println("✗ Chat model failed: " + e.getMessage());
                System.err.println("Make sure llama3.2 model is installed");
                return;
            }
            
            // Initialize Milvus Embedding Store
            System.out.println("Initializing Milvus Embedding Store...");
            EmbeddingStore<TextSegment> embeddingStore;
            try {
                embeddingStore = MilvusConfig.createMilvusEmbeddingStore();
                System.out.println("✓ Milvus connection established!");
            } catch (Exception e) {
                System.err.println("✗ Milvus connection failed: " + e.getMessage());
                System.err.println("Make sure Milvus is running on port 19530");
                return;
            }
            
            // Create DocumentLoader
            System.out.println("Ingesting documents...");
            DocumentLoader documentLoader = new DocumentLoader(embeddingModel, embeddingStore);
            
            try {
                documentLoader.ingestDocument();
                System.out.println("✓ Document ingestion completed!");
            } catch (Exception e) {
                System.err.println("✗ Document ingestion failed: " + e.getMessage());
                return;
            }
            
            // Initialize RAG Service
            System.out.println("Initializing RAG Service...");
            RAGService ragService = new RAGService(embeddingStore, embeddingModel, chatLanguageModel);
            
            // Example queries
            String query = "What is the capital of France?";
            
            // Get the answer from the RAG service
            System.out.println("\n" + "=".repeat(50));
            System.out.println("Query: " + query);
            System.out.println("=".repeat(50));
            
            String answer = ragService.answer(query);
            System.out.println("Answer: " + answer);
            System.out.println("=".repeat(50));
            
        } catch (Exception e) {
            System.err.println("Error during application execution: " + e.getMessage());
            e.printStackTrace();
        }
    }
}