package com.yang.yangaiagent.rag;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.markdown.MarkdownDocumentReader;
import org.springframework.ai.reader.markdown.config.MarkdownDocumentReaderConfig;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 文档收集以及切割
 */
@Component
@Slf4j
public class LoveAppDocumentLoader {

    private final ResourcePatternResolver resourcePatternResolver;

    public LoveAppDocumentLoader(ResourcePatternResolver resolver) {
        this.resourcePatternResolver = resolver;
    }

    /**
     * 加载 Markdown 文件
     *
     * @return 文档列表
     */
    public List<Document> loadMarkdown() {
        List<Document> result = new ArrayList<>();
        //load md
        try {
            Resource[] resources = resourcePatternResolver.getResources("classpath:document/*.md");
            for(Resource resource : resources) {
                String filename = resource.getFilename();
                MarkdownDocumentReaderConfig config = MarkdownDocumentReaderConfig.builder()
                        .withHorizontalRuleCreateDocument(true)
                        .withIncludeCodeBlock(false)
                        .withIncludeBlockquote(false)
                        .withAdditionalMetadata("filename", filename)
                        .build();
                MarkdownDocumentReader markdownDocumentReader = new MarkdownDocumentReader(resource, config);
                result.addAll(markdownDocumentReader.get());
            }
        } catch (IOException e) {
            log.warn("Markdown 文件加载失败, 失败原因::{}", e);
        }
        return result;
    }
}
