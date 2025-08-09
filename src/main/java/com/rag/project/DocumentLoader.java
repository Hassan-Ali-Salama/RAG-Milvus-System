package com.rag.project;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.document.parser.TextDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class DocumentLoader {
    
    private final EmbeddingModel embeddingModel;
    private final EmbeddingStore<TextSegment> embeddingStore;
    
    public DocumentLoader(EmbeddingModel embeddingModel, EmbeddingStore<TextSegment> embeddingStore) {
        this.embeddingModel = embeddingModel;
        this.embeddingStore = embeddingStore;
    }
    
    public void ingestDocument() throws IOException {
        Path path = Paths.get("documents/data.txt");
        
        // Check if file exists
        if (!path.toFile().exists()) {
            System.err.println("File not found: " + path.toAbsolutePath());
            return;
        }
        
        System.out.println("Loading document from: " + path.toAbsolutePath());
        Document document = FileSystemDocumentLoader.loadDocument(path, new TextDocumentParser());
        
        // Split the document into chunks
        var splitter = DocumentSplitters.recursive(500, 100);
        List<TextSegment> chunks = splitter.split(document);
        
        System.out.println("Document split into " + chunks.size() + " chunks");
        
        // Generate embeddings and store them - Fixed for 0.26.1
        for (int i = 0; i < chunks.size(); i++) {
            TextSegment chunk = chunks.get(i);
            System.out.println("Processing chunk " + (i + 1) + "/" + chunks.size());
            
            // Simple approach for 0.26.1
            var embedding = embeddingModel.embed(chunk.text()).content();
            embeddingStore.add(embedding, chunk);
        }
        
        System.out.println("Document ingested successfully into Milvus!");
    }
}