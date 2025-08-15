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
            server = HttpServer.create(new InetSocketAddress(8081), 0);
            server.createContext("/", new HomeHandler());
            server.createContext("/ask", new QueryHandler());
            server.createContext("/health", new HealthHandler());
            server.setExecutor(null);
            
            server.start();
            System.out.println("🌐 RAG Web Server started at http://localhost:8081");
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
        
        String ollamaHost = "http://localhost:11434";
        
        EmbeddingModel embeddingModel = OllamaEmbeddingModel.builder()
                .baseUrl(ollamaHost)
                .modelName("nomic-embed-text:latest")
                .timeout(Duration.ofMinutes(3))
                .build();
        
        ChatLanguageModel chatModel = OllamaChatModel.builder()
                .baseUrl(ollamaHost)
                .modelName("llama3.2:latest")
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
"<html lang=\"en\">" +
"<head>" +
"<meta charset=\"UTF-8\">" +
"<title>RAG System</title>" +
"<style>" +
"body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background: linear-gradient(120deg, #1d2b64, #f8cdda); color: #333; max-width: 900px; margin: 0 auto; padding: 20px; }" +
"h1 { text-align: center; color: white; text-shadow: 1px 1px 3px rgba(0,0,0,0.4); animation: fadeIn 1s ease-in-out; }" +
".chat-box { background: rgba(255, 255, 255, 0.9); border-radius: 12px; padding: 20px; min-height: 350px; margin: 20px 0; box-shadow: 0 4px 20px rgba(0,0,0,0.15); overflow-y: auto; animation: fadeInUp 0.8s ease; }" +
".chat-message { margin: 10px 0; padding: 12px 16px; border-radius: 8px; animation: fadeIn 0.5s ease; }" +
".user { background: #4e54c8; color: white; align-self: flex-end; }" +
".ai { background: #f0f0f0; border: 1px solid #ddd; }" +
".input-container { display: flex; justify-content: center; margin-top: 10px; }" +
"input[type=\"text\"] { flex: 1; padding: 12px; border: 1px solid #ccc; border-radius: 8px; font-size: 14px; outline: none; transition: 0.3s; }" +
"input[type=\"text\"]:focus { border-color: #4e54c8; box-shadow: 0 0 8px rgba(78, 84, 200, 0.5); }" +
"button { padding: 12px 20px; margin-left: 10px; background: #4e54c8; color: white; border: none; border-radius: 8px; cursor: pointer; transition: 0.3s; }" +
"button:hover { background: #3b40a4; transform: scale(1.05); }" +
"@keyframes fadeIn { from { opacity: 0; transform: translateY(10px); } to { opacity: 1; transform: translateY(0); } }" +
"@keyframes fadeInUp { from { opacity: 0; transform: translateY(30px); } to { opacity: 1; transform: translateY(0); } }" +
"</style>" +
"</head>" +
"<body>" +
"<h1>🤖 RAG System - Ask Questions</h1>" +
"<div class=\"chat-box\" id=\"chat\">" +
"<p>Ask me anything about the documents!</p>" +
"</div>" +
"<div class=\"input-container\">" +
"<input type=\"text\" id=\"queryInput\" placeholder=\"Enter your question here...\" />" +
"<button onclick=\"askQuestion()\">Ask</button>" +
"</div>" +
"<script>" +
"async function askQuestion() {" +
"const input = document.getElementById('queryInput');" +
"const query = input.value.trim();" +
"if (!query) return;" +
"const chat = document.getElementById('chat');" +
"chat.innerHTML += '<div class=\"chat-message user\"><strong>You:</strong> ' + query + '</div>';" +
"try {" +
"const response = await fetch('/ask', {" +
"method: 'POST'," +
"headers: { 'Content-Type': 'application/json' }," +
"body: JSON.stringify({ query: query })" +
"});" +
"const result = await response.json();" +
"chat.innerHTML += '<div class=\"chat-message ai\"><strong>AI:</strong> ' + result.answer + '</div>';" +
"} catch (error) {" +
"chat.innerHTML += '<div class=\"chat-message ai\"><strong>Error:</strong> ' + error.message + '</div>';" +
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