package com.yang.yangaiagent.config;

import com.yang.yangaiagent.rag.LoveAppDocumentLoader;
import jakarta.annotation.Resource;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * 基于内存的初始化向量
 */

@Configuration
public class LoveAppVectorStoreConfig {

    @Resource
    private LoveAppDocumentLoader loveAppDocumentLoader;


    @Bean
    public VectorStore loveAppVectorStore(EmbeddingModel dashScopeEmbeddingModel) {
        SimpleVectorStore simpleVectorStore = SimpleVectorStore.builder(dashScopeEmbeddingModel)
                .build();
        //load file
        List<Document> documents = loveAppDocumentLoader.loadMarkdown();
        simpleVectorStore.add(documents);
        return simpleVectorStore;

    }

}
