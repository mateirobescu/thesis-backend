package com.mateirobescu.thesis.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.beans.PropertyAccessorFactory;

public class FieldMatchValidator implements ConstraintValidator<FieldMatch, Object> {

    private String first;
    private String second;

    @Override
    public void initialize(FieldMatch annotation) {
        this.first = annotation.first();
        this.second = annotation.second();
    }

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        if (value == null) return true;

        var accessor = PropertyAccessorFactory.forDirectFieldAccess(value);
        Object a = accessor.getPropertyValue(first);
        Object b = accessor.getPropertyValue(second);

        if (a == null || b == null) return true;

        boolean matches = a.equals(b);
        if (!matches) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(context.getDefaultConstraintMessageTemplate())
                    .addPropertyNode(second)
                    .addConstraintViolation();
        }
        return matches;
    }
}