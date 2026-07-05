package org.catmunity.config;

import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.elasticsearch.ElasticsearchVectorStore;
import org.springframework.ai.vectorstore.elasticsearch.ElasticsearchVectorStoreOptions;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

@Configuration
public class ElasticsearchVectorStoreConfig {

    @Value("${spring.elasticsearch.host:localhost}")
    private String host;

    @Value("${spring.elasticsearch.port:9200}")
    private int port;

    @Value("${spring.elasticsearch.username:}")
    private String username;

    @Value("${spring.elasticsearch.password:}")
    private String password;

    @Value("${spring.ai.vectorstore.elasticsearch.indices.catmunity:catmunity}")
    private String catmunityIndexName;

    @Value("${spring.ai.vectorstore.elasticsearch.indices.catprofile:catprofile}")
    private String catprofileIndexName;

    @Value("${spring.ai.vectorstore.elasticsearch.initialize-schema:true}")
    private boolean initializeSchema;

    @Value("${spring.ai.vectorstore.elasticsearch.dimensions:1536}")
    private int dimensions;

    @Value("${spring.ai.vectorstore.elasticsearch.similarity:cosine}")
    private String similarity;

    @Bean
    public RestClient elasticsearchRestClient() {
        RestClientBuilder builder = RestClient.builder(new HttpHost(host, port, "http"));
        
        if (StringUtils.hasText(username) && StringUtils.hasText(password)) {
            BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
            credentialsProvider.setCredentials(AuthScope.ANY,
                    new UsernamePasswordCredentials(username, password));
            builder.setHttpClientConfigCallback(httpClientBuilder ->
                    httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider));
        }
        
        return builder.build();
    }

    @Bean(name = "catmunityVectorStore")
    public ElasticsearchVectorStore catmunityVectorStore(
            RestClient restClient,
            @Qualifier("openAiEmbeddingModel") EmbeddingModel embeddingModel) {
        ElasticsearchVectorStoreOptions options = new ElasticsearchVectorStoreOptions();
        options.setIndexName(catmunityIndexName);
        options.setDimensions(dimensions);

        return ElasticsearchVectorStore.builder(restClient, embeddingModel)
                .options(options)
                .initializeSchema(initializeSchema)
                .build();
    }

    @Bean(name = "catprofileVectorStore")
    public ElasticsearchVectorStore catprofileVectorStore(
            RestClient restClient,
            @Qualifier("openAiEmbeddingModel") EmbeddingModel embeddingModel) {
        ElasticsearchVectorStoreOptions options = new ElasticsearchVectorStoreOptions();
        options.setIndexName(catprofileIndexName);
        options.setDimensions(dimensions);

        return ElasticsearchVectorStore.builder(restClient, embeddingModel)
                .options(options)
                .initializeSchema(initializeSchema)
                .build();
    }
}