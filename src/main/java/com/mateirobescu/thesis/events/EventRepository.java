package com.mateirobescu.thesis.events;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface EventRepository extends JpaRepository<Event, UUID> {

    List<Event> findByFile_IdAndSeqGreaterThan(UUID fileId, Long seqIsGreaterThan);
}
