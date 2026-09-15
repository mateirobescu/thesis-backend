package com.mateirobescu.thesis.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;

public class ValidPathValidator implements ConstraintValidator<ValidPath, String> {
    @Override
    public boolean isValid(String path, ConstraintValidatorContext context) {
        if (path == null)
            return true;

        if (path.contains("\\"))
            return false;

        if(path.startsWith("/") || path.endsWith("/"))
            return false;

        if (path.contains("//")) {
            return false;
        }

        try {
            Path pathObj = Path.of(path);
            Path normalizedPath = pathObj.normalize();
            return pathObj.equals(normalizedPath);
        } catch (InvalidPathException e) {
            return false;
        }

    }
}
