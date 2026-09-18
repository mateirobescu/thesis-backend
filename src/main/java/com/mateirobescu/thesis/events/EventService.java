package com.mateirobescu.thesis.events;

import com.mateirobescu.thesis.files.File;
import com.mateirobescu.thesis.files.FileService;
import com.mateirobescu.thesis.projects.Project;
import com.mateirobescu.thesis.projects.ProjectService;
import com.mateirobescu.thesis.users.User;
import com.mateirobescu.thesis.users.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
//TODO different mapper?
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.UUID;

@Service
@Slf4j
class EventService {

    EventRepository eventRepository;
    FileService fileService;
    ProjectService projectService;
    UserService userService;
    ObjectMapper objectMapper;

    public EventService(
            EventRepository eventRepository,
            FileService fileService,
            ProjectService projectService,
            UserService userService,
            ObjectMapper objectMapper) {
        this.eventRepository = eventRepository;
        this.fileService = fileService;
        this.projectService = projectService;
        this.userService = userService;
        this.objectMapper = objectMapper;
    }

    //TODO needs more auth and more stuff but conceptually right
    @Transactional
    public Event createFileEvent(FileEventCreateCommand command) {
        User user = userService.getUserById(command.envelope().userId());

        Project project = projectService.getProjectWithNewSeq(command.envelope().projectId());

        Event event = Event.builder()
                .project(project)
                .user(user)
                .seq(project.getSeq())
                .clientSeq(command.envelope().clientSeq())
                .clientTimestamp(command.envelope().clientTimestamp())
                .type(command.envelope().type())
                .payload(objectMapper.valueToTree(command.payload()))
                .build();

        Event savedEvent = eventRepository.save(event);
        log.info("Event created id={}", savedEvent.getId());
        return savedEvent;
    }

    //TODO maybe throw error for non project file
    public List<Event> getProjectEventsWithSeqGreaterThan(UUID projectId, Long seq) {
        return eventRepository.findByProject_IdAndSeqGreaterThanOrderBySeqAsc(projectId, seq);
    }

}
