package com.github.zikani03.jdbi;

import com.google.common.base.MoreObjects;

import javax.validation.ConstraintViolation;
import javax.validation.ValidationException;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public final class Validation {
    static final ValidatorFactory factory = javax.validation.Validation.buildDefaultValidatorFactory();

    /**
     * Validates arguments via Hibernate validator and throws an {@link ValidationException}
     * if the object fails validation otherwise returns the object itself
     */
    public static final Object valid(Object object) {
        Validation.throwOnFailedValidation(object);
        return object;
    }

    /**
     * Validates a value and throws exception if there were any validation errors.
     *
     * @param value
     * @param groups
     * @param <T>
     * @throws Exception
     */
    public static <T> void throwOnFailedValidation(T value, Class<?>... groups) throws ValidationException {
        MoreObjects.ToStringHelper helper = MoreObjects.toStringHelper(Map.class);
        Map<String, String> validationErrors = validate(value, groups);
        if (! validationErrors.isEmpty()) {
            validationErrors.entrySet()
                    .stream()
                    .forEach(entry -> helper.add(entry.getKey(), entry.getValue()));
            throw new ValidationException("Entity contains validation errors. Errors: " + helper.toString());
        }
    }


    /**
     * Validate an object of a type and return a map of the errors.
     * The keys are the names of the properties with validation errors.
     *
     * @param value
     * @param groups
     * @param <T>
     * @return
     */
    public static <T> Map<String, String> validate(T value, Class<?>... groups) {
        Validator validator = factory.getValidator();
        if (!Objects.isNull(groups) || groups.length > 0) {
            return validator.validate(value, groups).stream()
                    .collect(Collectors.toMap(cv -> cv.getPropertyPath().toString(), ConstraintViolation::getMessage));
        }
        return validator.validate(value).stream()
                .collect(Collectors.toMap(cv -> cv.getPropertyPath().toString(), ConstraintViolation::getMessage));
    }
}
