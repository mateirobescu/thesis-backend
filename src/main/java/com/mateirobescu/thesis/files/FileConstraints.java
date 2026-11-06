package com.mateirobescu.thesis.files;

/**
 * Names of the constraints on the {@code files} table that the code translates into API errors.
 */
public final class FileConstraints {

    private FileConstraints() {
    }

    /**
     * Partial unique index on (project, path) among files that are not soft-deleted, defined in the Flyway migration.
     */
    public static final String PROJECT_PATH_UNIQUE_CONSTRAINT = "uk_files_project_path_active";
}
