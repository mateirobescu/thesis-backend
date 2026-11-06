package com.mateirobescu.thesis.operations;

import com.mateirobescu.thesis.projects.Project;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface OperationRepository extends JpaRepository<Operation, UUID> {

    Optional<Operation> findByProjectAndSeq(Project project, Long seq);

    List<Operation> findByProjectAndSeqGreaterThanOrderBySeqAsc(Project project, Long projectSeqIsGreaterThan);
}
