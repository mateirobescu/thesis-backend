package com.mateirobescu.thesis.files;

import com.mateirobescu.thesis.users.User;

public record FilePatch (
        String path,
        User owner //TODO maybe delete this too
) { }
