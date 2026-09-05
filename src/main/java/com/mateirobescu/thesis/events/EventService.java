package com.mateirobescu.thesis.events;

import com.mateirobescu.thesis.files.File;
import com.mateirobescu.thesis.files.FileService;
import com.mateirobescu.thesis.projects.Project;
import com.mateirobescu.thesis.projects.ProjectService;
import com.mateirobescu.thesis.users.User;
import com.mateirobescu.thesis.users.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
class EventService {

    EventRepository eventRepository;
    FileService fileService;
    ProjectService projectService;
    UserService userService;

    public EventService(EventRepository eventRepository, FileService fileService, ProjectService projectService, UserService userService) {
        this.eventRepository = eventRepository;
        this.fileService = fileService;
        this.projectService = projectService;
        this.userService = userService;
    }

    //TODO needs more auth and more stuff but conceptually right
    @Transactional
    public Event createEvent(EventCreateCommand command) {
        User user = userService.getUserById(command.userId());

        File file = fileService.getFileWithNewSeq(command.fileId());
        Project project = projectService.getProjectWithNewSeq(file.getProject().getId());

        Event event = Event.builder()
                .file(file)
                .user(user)
                .seq(file.getSeq())
                .projectSeq(project.getSeq())
                .clientSeq(command.clientSeq())
                .clientProjectSeq(command.clientProjectSeq())
                .clientTimestamp(command.clientTimestamp())
                .char_offset(command.char_offset())
                .length(command.length())
                .chars(command.chars())
                .build();

        return eventRepository.save(event);
    }

    public List<Event> getFileEventsWithSeqGreaterThan(UUID fileId, Long seq) {
        return eventRepository.findByFile_IdAndSeqGreaterThan(fileId, seq);
    }

}
