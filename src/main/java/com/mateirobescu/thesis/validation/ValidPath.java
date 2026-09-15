package com.mateirobescu.thesis.validation;


import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
@Constraint( validatedBy = ValidPathValidator.class)
public @interface ValidPath {
    //TODO maybe refine this message
    String message() default "This must be a valid path!";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
