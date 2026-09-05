package com.mateirobescu.thesis.files;

import com.mateirobescu.thesis.projects.Project;
import com.mateirobescu.thesis.users.User;
import com.mateirobescu.thesis.workspaces.Workspace;

public record FilePatch (
        Project project,
        String filename,
        String path,
        User owner
) { }
