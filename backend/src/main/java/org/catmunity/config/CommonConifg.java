package org.catmunity.config;

import org.catmunity.advisor.ContextChatMemoryAdvisor;
import org.catmunity.advisor.DatabaseChatMemory;
import org.catmunity.constants.SystemConstant;
import org.catmunity.tool.CatTool;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.QuestionAnswerAdvisor;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CommonConifg {

    @Value("${spring.ai.vectorstore.elasticsearch.top-k:3}")
    private int topK;

    @Value("${spring.ai.vectorstore.elasticsearch.similarity-threshold:0.7}")
    private double similarityThreshold;

    @Value("${spring.ai.chat.options.max-tokens:2048}")
    private int maxTokens;

    @Value("${spring.ai.chat.options.temperature:0.3}")
    private double temperature;

    @Bean
    public ChatClient chatClient(
            OpenAiChatModel model,
            DatabaseChatMemory chatMemory,
            CatTool catTool,
            @Qualifier("catmunityVectorStore") VectorStore vectorStore,
            ContextChatMemoryAdvisor contextChatMemoryAdvisor) {

        return ChatClient.builder(model)
                .defaultSystem(SystemConstant.SYSTEM_QA_TEMPLATE)
                .defaultOptions(OpenAiChatOptions.builder()
                        .maxTokens(maxTokens)
                        .temperature(temperature)
                        .build())
                .defaultTools(catTool)
                .defaultAdvisors(
                        contextChatMemoryAdvisor,
                        new MessageChatMemoryAdvisor(chatMemory),
                        new QuestionAnswerAdvisor(
                                vectorStore,
                                SearchRequest.builder()
                                        .topK(topK)
                                        .similarityThreshold(similarityThreshold)
                                        .build()
                        )
                )
                .build();
    }

    @Bean
    public ChatClient moderatorChatClient(OpenAiChatModel model) {
        return ChatClient.builder(model)
                .defaultSystem(SystemConstant.MODERATION_PROMPT)
                .build();
    }

    @Bean
    public ChatClient SummaryChatClient(OpenAiChatModel model) {
        return ChatClient.builder(model)
                .defaultSystem(SystemConstant.SUMMARY_PROMPT)
                .build();
    }
}