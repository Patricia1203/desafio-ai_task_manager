package com.desafio.taskmanager.common.error;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.desafio.taskmanager.common.web.TraceIdFilter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * ERR-02 e ERR-06: toda resposta de erro e ProblemDetail (RFC 7807) e nenhum
 * erro vaza stack trace, nome de classe, SQL ou caminho de arquivo.
 *
 * <p>Cobre tambem os status de infraestrutura do MVC. Sem handler proprio,
 * {@code NoResourceFoundException}, {@code HttpRequestMethodNotSupportedException}
 * e {@code HttpMediaTypeNotSupportedException} caem no {@code Exception.class} e
 * saem como 500.
 */
@WebMvcTest(controllers = ErrorProbeController.class)
@Import(TraceIdFilter.class)
class GlobalExceptionHandlerTest {

    private static final String PROBLEM_JSON = MediaType.APPLICATION_PROBLEM_JSON_VALUE;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void beanValidationDevolve400ComListaDeCampos() throws Exception {
        mockMvc.perform(post("/__test/validacao")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"field\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value(containsString("validacao")))
                .andExpect(jsonPath("$.title").exists())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors", hasSize(1)))
                .andExpect(jsonPath("$.errors[0].field").value("field"))
                .andExpect(jsonPath("$.errors[0].reason").value("campo obrigatorio"));
    }

    @Test
    void recursoInexistenteDevolve404() throws Exception {
        mockMvc.perform(get("/__test/inexistente"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.type").value(containsString("nao-encontrado")))
                .andExpect(jsonPath("$.detail").value(containsString("Tarefa")));
    }

    @Test
    void regraDeNegocioDevolve422() throws Exception {
        mockMvc.perform(get("/__test/regra"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.type").value(containsString("regra-de-negocio")));
    }

    @Test
    void erroGenericoDevolve500ComMensagemFixaESemStackTrace() throws Exception {
        mockMvc.perform(get("/__test/inesperado"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.detail").value("Erro interno. Tente novamente mais tarde."))
                // a mensagem original nao pode aparecer em lugar nenhum
                .andExpect(jsonPath("$.detail", not(containsString("jdbc:postgresql"))))
                .andExpect(jsonPath("$..trace").doesNotExist())
                .andExpect(jsonPath("$..exception").doesNotExist())
                .andExpect(content().string(not(containsString("IllegalStateException"))))
                .andExpect(content().string(not(containsString("at com.desafio"))))
                .andExpect(content().string(not(containsString("\\tat "))));
    }

    @Test
    void jsonMalformadoDevolve400SemDetalheDoParser() throws Exception {
        mockMvc.perform(post("/__test/validacao")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ isso nao eh json "))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").value("Corpo ou parametro de requisicao invalido"))
                .andExpect(content().string(not(containsString("com.fasterxml"))));
    }

    @Test
    void todaRespostaDeErroTemOsCamposObrigatoriosDoRfc7807() throws Exception {
        mockMvc.perform(get("/__test/inexistente"))
                .andExpect(jsonPath("$.type").exists())
                .andExpect(jsonPath("$.title").exists())
                .andExpect(jsonPath("$.status").exists())
                .andExpect(jsonPath("$.detail").exists());
    }

    @Test
    void rotaInexistenteDevolve404ENao500() throws Exception {
        mockMvc.perform(get("/__test/rota-que-nao-existe"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.type").value(containsString("nao-encontrado")));
    }

    @Test
    void metodoNaoPermitidoDevolve405ENao500() throws Exception {
        mockMvc.perform(put("/__test/metodo"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(405))
                .andExpect(jsonPath("$.type").value(containsString("metodo-nao-permitido")));
    }

    @Test
    void contentTypeInvalidoDevolve415ENao500() throws Exception {
        mockMvc.perform(post("/__test/tipo")
                        .contentType(MediaType.APPLICATION_XML)
                        .content("<xml/>"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(415))
                .andExpect(jsonPath("$.type").value(containsString("tipo-nao-suportado")));
    }

    @Test
    void falhaDeBancoDevolve500ComMensagemFixaESemSql() throws Exception {
        mockMvc.perform(get("/__test/banco"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.detail").value("Erro interno. Tente novamente mais tarde."))
                .andExpect(jsonPath("$.type").value(containsString("banco-indisponivel")))
                .andExpect(content().string(not(containsString("jdbc:postgresql"))))
                .andExpect(content().string(not(containsString("taskmanager"))));
    }

    @Test
    void deleteNaRotaQueSoAceitaGetDevolve405() throws Exception {
        mockMvc.perform(delete("/__test/metodo"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.status").value(405));
    }

    @Test
    void falhaDeComunicacaoComOLLmDevolve502SemVazarDetalhe() throws Exception {
        mockMvc.perform(get("/__test/llm-comunicacao"))
                .andExpect(status().isBadGateway())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(502))
                .andExpect(jsonPath("$.title").value("Falha na comunicacao com a IA"))
                .andExpect(jsonPath("$.type").value(containsString("erro-de-comunicacao-com-llm")))
                .andExpect(jsonPath("$.code").doesNotExist())
                // herda traceId e timestamp como qualquer outra resposta de erro
                .andExpect(jsonPath("$.traceId").value(matchesPattern("[0-9a-f]{16}")))
                .andExpect(jsonPath("$.timestamp").value(
                        matchesPattern("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}.*")))
                .andExpect(content().string(not(containsString("at com.desafio"))))
                .andExpect(content().string(not(containsString("\\tat "))));
    }

    @Test
    void llmIndisponivelDevolve503ComDetailFixa() throws Exception {
        mockMvc.perform(get("/__test/llm-indisponivel"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(503))
                .andExpect(jsonPath("$.title").value("IA indisponivel"))
                .andExpect(jsonPath("$.type").value(containsString("llm-indisponivel")))
                .andExpect(jsonPath("$.detail").value("O servico de IA esta indisponivel no momento"))
                .andExpect(jsonPath("$.traceId").value(matchesPattern("[0-9a-f]{16}")))
                .andExpect(jsonPath("$.timestamp").value(
                        matchesPattern("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}.*")))
                .andExpect(content().string(not(containsString("at com.desafio"))))
                .andExpect(content().string(not(containsString("\\tat "))));
    }

    @Test
    void respostaInvalidaDaIaDevolve502ComOCodigoLlmInvalidResponse() throws Exception {
        mockMvc.perform(get("/__test/llm-invalida"))
                .andExpect(status().isBadGateway())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(502))
                .andExpect(jsonPath("$.type").value(containsString("resposta-llm-invalida")))
                .andExpect(jsonPath("$.title").value("Resposta invalida da IA"))
                .andExpect(jsonPath("$.code").value("LLM_INVALID_RESPONSE"))
                .andExpect(jsonPath("$.traceId").value(matchesPattern("[0-9a-f]{16}")))
                .andExpect(jsonPath("$.timestamp").value(
                        matchesPattern("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}.*")))
                .andExpect(content().string(not(containsString("at com.desafio"))))
                .andExpect(content().string(not(containsString("\\tat "))));
    }

    @Test
    void todaRespostaDeErroLevaOTraceIdDoMdc() throws Exception {
        mockMvc.perform(get("/__test/inexistente"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.traceId").exists())
                .andExpect(jsonPath("$.traceId").value(matchesPattern("[0-9a-f]{16}")))
                // timestamp em ISO-8601 UTC, legivel por qualquer cliente
                .andExpect(jsonPath("$.timestamp").value(
                        matchesPattern("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}.*")));
    }

    @Test
    void sucessoTambemExpoemOTraceIdNoHeader() throws Exception {
        mockMvc.perform(get("/__test/regra"))
                .andExpect(header().string("X-Trace-Id", matchesPattern("[0-9a-f]{16}")));
    }

    @Test
    void traceIdNaoVazaEntreRequisicoesDiferentes() throws Exception {
        String traceId = mockMvc.perform(get("/__test/regra"))
                .andExpect(header().exists("X-Trace-Id"))
                .andReturn().getResponse().getHeader("X-Trace-Id");

        // segunda requisicao tem outro id: o traceId nao vaza entre pedidos
        String outro = mockMvc.perform(get("/__test/regra"))
                .andReturn().getResponse().getHeader("X-Trace-Id");

        assertThat(traceId).isNotEqualTo(outro);
    }
}