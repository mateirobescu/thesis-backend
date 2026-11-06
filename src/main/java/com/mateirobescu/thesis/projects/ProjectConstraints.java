package com.mateirobescu.thesis.projects;

/**
 * Names of the constraints on the {@code projects} table that the code translates into API errors.
 */
public final class ProjectConstraints {

    private ProjectConstraints() {
    }

    /**
     * Partial unique index on (workspace, path, name) among projects that are not soft-deleted, defined in the Flyway migration.
     */
    public static final String WORKSPACE_PATH_NAME_UNIQUE_CONSTRAINT = "uk_projects_workspace_path_name_active";
}
