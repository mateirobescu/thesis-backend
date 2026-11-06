package com.mateirobescu.thesis.workspaces;

import com.mateirobescu.thesis.users.User;

import java.util.UUID;

public record WorkspacePatch(
        String name,
        User owner
) { }
