package com.desafio.taskmanager.ai.adapter.config;

import com.desafio.taskmanager.ai.adapter.SpringAiTaskAiAdapter;
import com.desafio.taskmanager.ai.application.LlmResponseValidator;
import com.desafio.taskmanager.ai.port.TaskAiPort;
import com.desafio.taskmanager.common.config.AssistantLimitsProperties;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.template.st.StTemplateRenderer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.restclient.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

/**
 * Ponto de montagem do provedor de IA (RNF-14: trocar provedor mexe so aqui e
 * no adaptador): ChatClient com o renderer dos prompts {@code .st}, a porta
 * implementada pelo adaptador e o timeout de {@code app.ai.timeout} aplicado
 * ao cliente HTTP que conversa com o Ollama.
 *
 * <p>O renderer e fixado como {@link StTemplateRenderer} porque os prompts
 * usam chaves como delimitador (o mesmo renderer do {@code PromptsTest}); o
 * default do ChatClient nao e assumido sem verificacao.
 *
 * <p>O timeout vira {@link RestClientCustomizer}, que o Boot aplica no
 * {@code RestClient.Builder} auto-configurado — o mesmo que o
 * {@code OllamaApi} consome. Sem isto {@code app.ai.timeout} seria
 * configuracao morta e a {@code SocketTimeoutException} mapeada pelo
 * adaptador nunca aconteceria de verdade.
 *
 * <p>Tambem registra {@link AssistantLimitsProperties} (F04): a lista de
 * arquivos de T-F04-02 nao previa uma classe de config propria para o
 * assistente, entao o mesmo {@code @EnableConfigurationProperties} de
 * {@code AiProperties} ganha a segunda entrada.
 */
@Configuration
@EnableConfigurationProperties({AiProperties.class, AssistantLimitsProperties.class})
public class AiAdapterConfig {

    @Bean
    ChatClient chatClient(ChatModel chatModel) {
        return ChatClient.builder(chatModel)
                .defaultTemplateRenderer(StTemplateRenderer.builder().build())
                .build();
    }

    @Bean
    TaskAiPort taskAiPort(ChatClient chatClient, LlmResponseValidator validator, AiProperties properties) {
        return new SpringAiTaskAiAdapter(chatClient, validator, properties);
    }

    @Bean
    RestClientCustomizer aiTimeoutCustomizer(AiProperties properties) {
        return builder -> {
            SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
            factory.setConnectTimeout(properties.timeout());
            factory.setReadTimeout(properties.timeout());
            builder.requestFactory(factory);
        };
    }
}
