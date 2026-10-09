package com.desafio.taskmanager.area.api;

import java.util.List;
import java.util.UUID;

import com.desafio.taskmanager.area.application.WorkAreaService;
import com.desafio.taskmanager.area.domain.WorkArea;
import com.desafio.taskmanager.area.infra.WorkAreaSummary;
import com.desafio.taskmanager.common.error.GlobalExceptionHandler;
import com.desafio.taskmanager.common.error.ResourceNotFoundException;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** TST-02: contrato HTTP de areas (F14) com o service mockado. */
@WebMvcTest(WorkAreaController.class)
@Import(GlobalExceptionHandler.class)
class WorkAreaControllerTest {

    private static final UUID ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    private static final byte[] PNG = {
            (byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A };

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private WorkAreaService service;

    private static WorkArea area() {
        WorkArea area = new WorkArea("Pessoal");
        area.setImage(PNG, "image/png");
        return area;
    }

    // --- GET /areas ---

    @Test
    void listarDevolveResumoSemBytes() throws Exception {
        WorkArea area = area();
        when(service.list(isNull()))
                .thenReturn(List.of(new WorkAreaSummary(area.getId(), "Pessoal", "image/png")));

        mockMvc.perform(get("/areas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(area.getId().toString()))
                .andExpect(jsonPath("$[0].title").value("Pessoal"))
                .andExpect(jsonPath("$[0].imageType").value("image/png"));

        verify(service).list(isNull());
    }

    @Test
    void listarPorTituloRepassaBuscaAoService() throws Exception {
        when(service.list("mor"))
                .thenReturn(List.of(new WorkAreaSummary(UUID.randomUUID(), "Moradia", null)));

        mockMvc.perform(get("/areas").param("title", "mor"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Moradia"));

        verify(service).list("mor");
    }

    // --- POST /areas (multipart) ---

    @Test
    void criarComImagemDevolve201ComLocation() throws Exception {
        WorkArea criada = area();
        when(service.create(anyString(), any(byte[].class), anyString())).thenReturn(criada);

        mockMvc.perform(multipart("/areas")
                        .file(arquivo("image", "foto.png", "image/png", PNG))
                        .param("title", "Pessoal"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.endsWith("/areas/" + criada.getId())))
                .andExpect(jsonPath("$.title").value("Pessoal"))
                .andExpect(jsonPath("$.imageType").value("image/png"));

        ArgumentCaptor<byte[]> bytes = ArgumentCaptor.forClass(byte[].class);
        verify(service).create(eq("Pessoal"), bytes.capture(), eq("image/png"));
        assertThat(bytes.getValue()).isEqualTo(PNG);
    }

    @Test
    void criarSemImagemDevolve201() throws Exception {
        when(service.create("Sem foto", null, null)).thenReturn(new WorkArea("Sem foto"));

        mockMvc.perform(multipart("/areas").param("title", "Sem foto"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Sem foto"))
                .andExpect(jsonPath("$.imageType").doesNotExist());

        verify(service).create(eq("Sem foto"), isNull(), isNull());
    }

    // --- PUT /areas/{id} (multipart) ---

    @Test
    void editarSoTituloMantemAFoto() throws Exception {
        WorkArea atualizada = area();
        atualizada.setTitle("Minhas tarefas");
        when(service.update(eq(ID), anyString(), isNull(), isNull(), anyBoolean())).thenReturn(atualizada);

        mockMvc.perform(putMultipart(ID)
                        .file(arquivoVazio("image"))
                        .param("title", "Minhas tarefas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Minhas tarefas"));

        verify(service).update(eq(ID), eq("Minhas tarefas"), isNull(), isNull(), eq(false));
    }

    @Test
    void editarComRemoveImageLimpaAFoto() throws Exception {
        when(service.update(eq(ID), isNull(), isNull(), isNull(), eq(true))).thenReturn(new WorkArea("Pessoal"));

        mockMvc.perform(putMultipart(ID).param("removeImage", "true"))
                .andExpect(status().isOk());

        verify(service).update(eq(ID), isNull(), isNull(), isNull(), eq(true));
    }

    // --- GET /areas/{id}/image ---

    @Test
    void buscarImagemDevolveBytesEContentType() throws Exception {
        when(service.findImage(ID)).thenReturn(new WorkAreaService.WorkAreaImage(PNG, "image/png"));

        mockMvc.perform(get("/areas/{id}/image", ID))
                .andExpect(status().isOk())
                .andExpect(content().bytes(PNG))
                .andExpect(header().string(org.springframework.http.HttpHeaders.CONTENT_TYPE,
                        org.hamcrest.Matchers.startsWith("image/png")))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("Content-Security-Policy", "default-src 'none'"));
    }

    @Test
    void buscarImagemDeIdInexistenteVira404() throws Exception {
        when(service.findImage(ID)).thenThrow(ResourceNotFoundException.of("imagem da area de trabalho", ID));

        mockMvc.perform(get("/areas/{id}/image", ID))
                .andExpect(status().isNotFound());
    }

    // --- DELETE /areas/{id} ---

    @Test
    void excluirDevolve204() throws Exception {
        mockMvc.perform(delete("/areas/{id}", ID))
                .andExpect(status().isNoContent());

        verify(service).delete(ID);
    }

    // --- helpers ---

    private static MockMultipartHttpServletRequestBuilder putMultipart(UUID id) {
        return multipart("/areas/{id}", id).with(request -> {
            request.setMethod("PUT");
            return request;
        });
    }

    private static org.springframework.mock.web.MockMultipartFile arquivo(String nome, String original,
            String contentType, byte[] bytes) {
        return new org.springframework.mock.web.MockMultipartFile(nome, original, contentType, bytes);
    }

    private static org.springframework.mock.web.MockMultipartFile arquivoVazio(String nome) {
        return new org.springframework.mock.web.MockMultipartFile(nome, new byte[0]);
    }
}