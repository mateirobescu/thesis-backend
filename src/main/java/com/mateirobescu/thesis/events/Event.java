package com.mateirobescu.thesis.events;

import com.mateirobescu.thesis.files.File;
import com.mateirobescu.thesis.users.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Table(name = "events")
public class Event {
    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "file_id", nullable = false)
    private File file;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @NotNull
    @Column(nullable = false)
    private Long seq;

    @NotNull
    @Column(nullable = false)
    private Long projectSeq;

    @NotNull
    @Column(nullable = false)
    private Long clientSeq;

    @NotNull
    @Column(nullable = false)
    private Long clientProjectSeq;

    @CreationTimestamp
    @Column(name = "server_timestamp", nullable = false, updatable = false)
    private Instant serverTimestamp;

    @NotNull
    @Column(nullable = false)
    private Instant clientTimestamp;

    //TODO these should be moved into the sub event called something like FileEvent
    @NotNull
    @Column(nullable = false)
    private Integer char_offset;

    @NotNull
    @Column(nullable = false)
    private Integer length;

    @NotNull
    @Column(nullable = false)
    private String chars;
}
