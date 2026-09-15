package com.mateirobescu.thesis.files;

import java.util.UUID;

public record FileResolveResult(
        File file,
        Boolean created
) {
}
