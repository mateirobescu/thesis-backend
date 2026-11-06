package com.mateirobescu.thesis.operations;

import com.mateirobescu.thesis.exception.NotFoundException;
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
public class OperationService {

    private final OperationRepository operationRepository;
    private final ProjectService projectService;
    private final UserService userService;
    private final ObjectMapper objectMapper;
    private final OperationPayloadParser payloadParser;

    public OperationService(
            OperationRepository operationRepository,
            ProjectService projectService,
            UserService userService,
            ObjectMapper objectMapper,
            OperationPayloadParser payloadParser) {
        this.operationRepository = operationRepository;
        this.projectService = projectService;
        this.userService = userService;
        this.objectMapper = objectMapper;
        this.payloadParser = payloadParser;
    }

    /**
     * Records an operation in a project. The payload is parsed and validated first, so malformed input fails before
     * anything is loaded or locked; the stored payload is the parsed one, which drops unknown fields.
     *
     * @throws com.mateirobescu.thesis.exception.InvalidPayloadException if the payload does not match the type
     */
    @Transactional
    public Operation createOperation(OperationCreateCommand command) {
        OperationPayload payload = payloadParser.parse(command.type(), command.payload());

        //TODO these 2 queries might be useless
        User user = userService.getUserById(command.userId());
        Project project = projectService.getProjectWithNewSeq(command.projectId());

        Operation operation = Operation.builder()
                .project(project)
                .user(user)
                .seq(project.getSeq())
                .clientSeq(command.clientSeq())
                .clientTimestamp(command.clientTimestamp())
                .type(command.type())
                //TODO maybe just do command.payload() but would have to check disadvantages
                .payload(objectMapper.valueToTree(payload))
                .build();

        //TODO maybe handle an error here?
        Operation savedOperation = operationRepository.save(operation);
        log.info("Operation created id={}", savedOperation.getId());
        return savedOperation;
    }

    /**
     * Returns the operation of a project with the given seq.
     *
     * @throws com.mateirobescu.thesis.exception.NotFoundException if there is no such project, including when it is
     *                                                             soft-deleted, or the project has no operation with
     *                                                             this seq
     */
    public Operation getOperationBySeq(UUID projectId, Long seq) {
        Project project = projectService.getProjectById(projectId);
        return operationRepository.findByProjectAndSeq(project, seq)
                .orElseThrow(() -> new NotFoundException("Operation", seq));
    }

    /**
     * Lists the operations of a project recorded after the given seq, oldest first.
     *
     * @throws com.mateirobescu.thesis.exception.NotFoundException if there is no such project, including when it is
     *                                                             soft-deleted
     */
    //TODO pagination should be handled with a limit, will se later exactly how
    public List<Operation> getProjectOperationsWithSeqGreaterThan(UUID projectId, Long seq) {
        Project project = projectService.getProjectById(projectId);
        return operationRepository.findByProjectAndSeqGreaterThanOrderBySeqAsc(project, seq);
    }

    //TODO also maybe file creation and deletion should maybe be an operation
}


