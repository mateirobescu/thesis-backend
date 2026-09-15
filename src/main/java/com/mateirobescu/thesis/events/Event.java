package com.mateirobescu.thesis.events;

import com.mateirobescu.thesis.projects.Project;
import com.mateirobescu.thesis.users.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;
import tools.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.UUID;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@ToString
@Table(
        name = "events",
        uniqueConstraints = {
               @UniqueConstraint(
                        name = "uk_project_seq",
                        columnNames = {"project_id", "project_seq"}
                ),
        }
)
public class Event {
    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false, updatable = false)
    private Project project;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    private User user;

    @NotNull
    @Column(nullable = false, updatable = false)
    private Long seq;

    @NotNull
    @Column(nullable = false, updatable = false)
    private Long clientSeq;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant timestamp;

    @NotNull
    @Column(nullable = false, updatable = false)
    private Instant clientTimestamp;

    @NotNull
    @Column(nullable = false, updatable = false)
    private EventType type;

    @NotNull
    @Column(columnDefinition = "jsonb", nullable = false, updatable = false)
    @JdbcTypeCode(SqlTypes.JSON)
    private JsonNode payload;

}
