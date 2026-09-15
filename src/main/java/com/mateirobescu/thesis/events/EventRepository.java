package com.mateirobescu.thesis.events;

import com.mateirobescu.thesis.projects.Project;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface EventRepository extends JpaRepository<Event, UUID> {

    List<Event> findByProject_IdAndSeqGreaterThanOrderBySeqAsc(UUID projectId, Long projectSeqIsGreaterThan);
}
