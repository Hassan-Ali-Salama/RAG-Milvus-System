package com.rag.project;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.milvus.MilvusEmbeddingStore;

public class MilvusConfig {
    
    public static EmbeddingStore<TextSegment> createMilvusEmbeddingStore() {
        return MilvusEmbeddingStore.builder()
                .host("localhost")
                .port(19530)
                .collectionName("rag_collection")
                .dimension(768) // For nomic-embed-text model
                .build();
    }
}