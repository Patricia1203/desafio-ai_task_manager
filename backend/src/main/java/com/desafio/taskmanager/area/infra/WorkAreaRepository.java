package com.desafio.taskmanager.area.infra;

import java.util.List;
import java.util.UUID;

import com.desafio.taskmanager.area.domain.WorkArea;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/** Acesso a tabela work_areas (F14). */
public interface WorkAreaRepository extends JpaRepository<WorkArea, UUID>, JpaSpecificationExecutor<WorkArea> {

    /** Listagem das areas em ordem alfabetica, para a grade ficar estavel. */
    List<WorkArea> findAllByOrderByTitleAsc();
}