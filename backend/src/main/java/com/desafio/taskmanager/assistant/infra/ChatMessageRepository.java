package com.desafio.taskmanager.assistant.infra;

import java.util.List;
import java.util.UUID;

import com.desafio.taskmanager.assistant.domain.ChatMessage;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Acesso a tabela {@code chat_messages} (F04).
 *
 * <p>A janela de historico pede so as ultimas mensagens, e o {@code Pageable}
 * e o teto: o banco corta, nao a aplicacao depois de materializar tudo. O
 * {@code id} e o desempate para que o relogio em microssegundos do Postgres
 * nao inverta mensagens gravadas no mesmo instante.
 *
 * <p>A consulta volta do mais novo para o mais velho porque e assim que o banco
 * corta com o {@code LIMIT}; quem monta o prompt inverte a lista (T-F06-06).
 */
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    @Query("""
            select m from ChatMessage m
            where m.conversationId = :conversationId
            order by m.createdAt desc, m.id desc
            """)
    List<ChatMessage> ultimasMensagens(
            @Param("conversationId") UUID conversationId, Pageable pageable);

    /**
     * F10: a conversa inteira na ordem cronologica, para restaura-la no
     * frontend ao escolher uma conversa do historico. O {@code id} e o
     * desempate para mensagens gravadas no mesmo instante.
     */
    @Query("""
            select m from ChatMessage m
            where m.conversationId = :conversationId
            order by m.createdAt asc, m.id asc
            """)
    List<ChatMessage> historicoCompleto(@Param("conversationId") UUID conversationId);
}