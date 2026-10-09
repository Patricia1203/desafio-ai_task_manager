package com.desafio.taskmanager.area.infra;

import java.util.List;
import java.util.UUID;

import com.desafio.taskmanager.area.domain.WorkArea;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WorkAreaRepository extends JpaRepository<WorkArea, UUID> {

    /**
     * Resumo de todas as areas em ordem alfabetica. A consulta seleciona so
     * {@code id/title/image_type}: a coluna {@code image} nao entra no SELECT.
     */
    @Query("select new com.desafio.taskmanager.area.infra.WorkAreaSummary(a.id, a.title, a.imageType) "
            + "from WorkArea a order by a.title asc")
    List<WorkAreaSummary> findAllSummaryOrderedByTitle();

    /**
     * Mesma projecao do resumo, filtrando por titulo. O {@code pattern} chega
     * pronto do service (com {@code %}/{@code _} escapados) para o LIKE tratar
     * o termo como literal.
     */
    @Query("select new com.desafio.taskmanager.area.infra.WorkAreaSummary(a.id, a.title, a.imageType) "
            + "from WorkArea a where lower(a.title) like :pattern escape '\\' order by a.title asc")
    List<WorkAreaSummary> findSummaryByTitle(@Param("pattern") String pattern);
}
