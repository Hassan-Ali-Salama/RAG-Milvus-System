package com.rag.project;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingMatch;

import java.util.List;

public class RAGService {
    
    private final EmbeddingStore<TextSegment> embeddingStore;
    private final EmbeddingModel embeddingModel;
    private final ChatLanguageModel chatLanguageModel;
    
    public RAGService(EmbeddingStore<TextSegment> embeddingStore, 
                     EmbeddingModel embeddingModel, 
                     ChatLanguageModel chatLanguageModel) {
        this.embeddingStore = embeddingStore;
        this.embeddingModel = embeddingModel;
        this.chatLanguageModel = chatLanguageModel;
    }
    
    public String answer(String userQuery) {
        try {
            // Generate embedding for the query
            var queryEmbedding = embeddingModel.embed(userQuery).content();
            
            // Search for relevant content - Fixed for 0.26.1
            List<EmbeddingMatch<TextSegment>> searchResults = embeddingStore.findRelevant(queryEmbedding, 3);
            
            // Build context from retrieved segments
            StringBuilder context = new StringBuilder();
            for (EmbeddingMatch<TextSegment> match : searchResults) {
                context.append(match.embedded().text()).append("\n\n");
            }
            
            // Generate response using chat model
            String prompt = String.format(
                "Based on the following context, answer the question: %s\n\nContext:\n%s",
                userQuery, context.toString()
            );
            
            return chatLanguageModel.generate(prompt);
            
        } catch (Exception e) {
            return "Error processing query: " + e.getMessage();
        }
    }
}