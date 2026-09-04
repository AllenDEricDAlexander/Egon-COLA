package top.egon.cola.archetype.source.agent.starter.config;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import top.egon.cola.archetype.source.agent.infrastructure.research.tool.McpResearchToolFactory;
import java.util.Arrays;

/** Host-only wiring for model, MCP tools and the fixed Agent Flow application graph. */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(DeepResearchConfigurationProperties.class)
public class DeepResearchAiConfiguration {

    @Bean(name = "mcpResearchToolFactory", destroyMethod = "close")
    @Profile("!test")
    @ConditionalOnMissingBean(name = "mcpResearchToolFactory")
    public McpResearchToolFactory mcpResearchToolFactory(DeepResearchConfigurationProperties properties) {
        return new McpResearchToolFactory(properties.search().mcp());
    }

    @Bean(name = "deepResearchSearchTools")
    @Profile("!test")
    @ConditionalOnMissingBean(name = "deepResearchSearchTools")
    public ToolCallback[] deepResearchSearchTools(
            @Qualifier("mcpResearchToolFactory") McpResearchToolFactory factory) {
        return factory.create();
    }

    @Bean(name = "deepResearchChatModel")
    @Profile("!test")
    @ConditionalOnMissingBean(name = "deepResearchChatModel")
    public ChatModel deepResearchChatModel(
            DeepResearchConfigurationProperties properties,
            @Qualifier("deepResearchSearchTools") ToolCallback[] searchTools) {
        DeepResearchConfigurationProperties.ModelProperties model = properties.model();
        OpenAiApi api = OpenAiApi.builder()
                .baseUrl(model.baseUrl())
                .apiKey(model.apiKey())
                .build();
        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .model(model.modelName())
                .toolCallbacks(Arrays.asList(searchTools.clone()))
                .build();
        return OpenAiChatModel.builder()
                .openAiApi(api)
                .defaultOptions(options)
                .build();
    }
}
