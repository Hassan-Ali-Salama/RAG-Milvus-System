package com.rag.project;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.ollama.OllamaEmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

public class RAGWebServer {
    
    private RAGService ragService;
    private HttpServer server;
    
    public static void main(String[] args) {
        new RAGWebServer().start();
    }
    
    public void start() {
        try {
            // Initialize RAG system
            initializeRAG();
            
            // Start web server
            server = HttpServer.create(new InetSocketAddress(8080), 0);
            server.createContext("/", new HomeHandler());
            server.createContext("/ask", new QueryHandler());
            server.createContext("/health", new HealthHandler());
            server.setExecutor(null);
            
            server.start();
            System.out.println("🌐 RAG Web Server started at http://localhost:8080");
            System.out.println("📋 Endpoints:");
            System.out.println("  - GET  / : Home page");
            System.out.println("  - POST /ask : Ask questions (JSON: {\"query\":\"your question\"})");
            System.out.println("  - GET  /health : Health check");
            
        } catch (Exception e) {
            System.err.println("Failed to start server: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void initializeRAG() {
        System.out.println("🚀 Initializing RAG system...");
        
        String ollamaHost = "http://localhost:11435";
        
        EmbeddingModel embeddingModel = OllamaEmbeddingModel.builder()
                .baseUrl(ollamaHost)
                .modelName("nomic-embed-text")
                .timeout(Duration.ofMinutes(3))
                .build();
        
        ChatLanguageModel chatModel = OllamaChatModel.builder()
                .baseUrl(ollamaHost)
                .modelName("llama3.2")
                .timeout(Duration.ofMinutes(3))
                .build();
        
        EmbeddingStore<TextSegment> embeddingStore = MilvusConfig.createMilvusEmbeddingStore();
        
        // Load documents
        DocumentLoader loader = new DocumentLoader(embeddingModel, embeddingStore);
        try {
            loader.ingestDocument();
        } catch (IOException e) {
            System.err.println("Failed to load documents: " + e.getMessage());
        }
        
        ragService = new RAGService(embeddingStore, embeddingModel, chatModel);
        System.out.println("✅ RAG system ready!");
    }
    
    class HomeHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String response = "<!DOCTYPE html>" +
                    "<html>" +
                    "<head>" +
                    "<title>RAG System</title>" +
                    "<meta charset=\"UTF-8\">" +
                    "<style>" +
                    "body { font-family: Arial, sans-serif; max-width: 800px; margin: 0 auto; padding: 20px; }" +
                    ".chat-box { border: 1px solid #ddd; padding: 20px; margin: 20px 0; min-height: 300px; }" +
                    "input[type=\"text\"] { width: 70%; padding: 10px; }" +
                    "button { padding: 10px 20px; margin-left: 10px; }" +
                    ".response { background: #f0f0f0; padding: 10px; margin: 10px 0; border-radius: 5px; }" +
                    "</style>" +
                    "</head>" +
                    "<body>" +
                    "<h1>🤖 RAG System - Ask Questions</h1>" +
                    "<div class=\"chat-box\" id=\"chat\">" +
                    "<p>Ask me anything about the documents!</p>" +
                    "</div>" +
                    "<div>" +
                    "<input type=\"text\" id=\"queryInput\" placeholder=\"Enter your question here...\" />" +
                    "<button onclick=\"askQuestion()\">Ask</button>" +
                    "</div>" +
                    "<script>" +
                    "async function askQuestion() {" +
                    "const input = document.getElementById('queryInput');" +
                    "const query = input.value.trim();" +
                    "if (!query) return;" +
                    "const chat = document.getElementById('chat');" +
                    "chat.innerHTML += '<div><strong>You:</strong> ' + query + '</div>';" +
                    "try {" +
                    "const response = await fetch('/ask', {" +
                    "method: 'POST'," +
                    "headers: { 'Content-Type': 'application/json' }," +
                    "body: JSON.stringify({ query: query })" +
                    "});" +
                    "const result = await response.json();" +
                    "chat.innerHTML += '<div class=\"response\"><strong>AI:</strong> ' + result.answer + '</div>';" +
                    "} catch (error) {" +
                    "chat.innerHTML += '<div class=\"response\"><strong>Error:</strong> ' + error.message + '</div>';" +
                    "}" +
                    "input.value = '';" +
                    "chat.scrollTop = chat.scrollHeight;" +
                    "}" +
                    "document.getElementById('queryInput').addEventListener('keypress', function(e) {" +
                    "if (e.key === 'Enter') askQuestion();" +
                    "});" +
                    "</script>" +
                    "</body>" +
                    "</html>";
            
            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
            exchange.sendResponseHeaders(200, response.getBytes(StandardCharsets.UTF_8).length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes(StandardCharsets.UTF_8));
            }
        }
    }
    
    class QueryHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equals(exchange.getRequestMethod())) {
                sendError(exchange, 405, "Method not allowed");
                return;
            }
            
            try {
                String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                
                String query = extractQuery(body);
                
                if (query == null || query.isEmpty()) {
                    sendError(exchange, 400, "Missing query parameter");
                    return;
                }
                
                String answer = ragService.answer(query);
                
                String response = String.format("{\"answer\":\"%s\"}", 
                    answer.replace("\"", "\\\"").replace("\n", "\\n"));
                
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
                exchange.sendResponseHeaders(200, response.getBytes(StandardCharsets.UTF_8).length);
                
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes(StandardCharsets.UTF_8));
                }
                
            } catch (Exception e) {
                sendError(exchange, 500, "Internal server error: " + e.getMessage());
            }
        }
        
        private String extractQuery(String json) {
            int start = json.indexOf("\"query\":");
            if (start == -1) return null;
            
            start = json.indexOf("\"", start + 8);
            if (start == -1) return null;
            
            int end = json.indexOf("\"", start + 1);
            if (end == -1) return null;
            
            return json.substring(start + 1, end);
        }
    }
    
    class HealthHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String response = "{\"status\":\"healthy\",\"service\":\"RAG System\"}";
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length());
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes());
            }
        }
    }
    
    private void sendError(HttpExchange exchange, int code, String message) throws IOException {
        String response = String.format("{\"error\":\"%s\"}", message);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(code, response.length());
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response.getBytes());
        }
    }
}