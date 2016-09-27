package com.github.zikani03.jdbi;

import com.google.common.base.MoreObjects;
import org.jdbi.v3.core.StatementContext;
import org.jdbi.v3.core.statement.StatementCustomizer;
import org.jdbi.v3.sqlobject.SqlStatementCustomizer;
import org.jdbi.v3.sqlobject.SqlStatementCustomizerFactory;
import org.jdbi.v3.sqlobject.SqlStatementCustomizingAnnotation;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import java.lang.annotation.*;
import java.lang.reflect.Method;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Target({ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@SqlStatementCustomizingAnnotation(Valid.Factory.class)
@Documented
public @interface Valid {
    /**
     * Optional Validation groups.
     *
     * @return
     */
    Class<?>[] groups() default {};

    class Factory implements SqlStatementCustomizerFactory {
        @Override
        public SqlStatementCustomizer createForMethod(Annotation annotation, Class<?> sqlObjectType, Method method) {
            return q -> {};
        }

        @Override
        public SqlStatementCustomizer createForParameter(Annotation annotation, Class<?> sqlObjectType, Method method, Object arg) {
            final Class<?>[] groups = ((Valid) annotation).groups();
            return create(arg, groups);
        }

        private SqlStatementCustomizer create(Object entity, Class<?>... groups) {
            return q -> q.addStatementCustomizer(new ValidatingCustomizer(entity, groups));
        }
    }

    /**
     * Statement Customizer that validates method parameters
     *
     */
    class ValidatingCustomizer implements StatementCustomizer {
        static final ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        final Object entity;
        final Class<?>[] groups;

        public ValidatingCustomizer(Object arg, Class<?>[] groups) {
            this.entity = arg;
            this.groups = groups;
        }

        @Override
        public void beforeExecution(PreparedStatement stmt, StatementContext ctx) throws SQLException {
            try {
                throwOnFailedValidation(entity, groups);
            } catch (Exception e) {
                throw new IllegalArgumentException("Entity contains validation errors. Errors: " + e.getMessage());
            }
        }

        /**
         * Validates a value and throws exception if there were any validation errors.
         *
         * @param value
         * @param groups
         * @param <T>
         * @throws Exception
         */
        private static <T> void throwOnFailedValidation(T value, Class<?>... groups) throws Exception {
            MoreObjects.ToStringHelper helper = MoreObjects.toStringHelper(Map.class);
            Map<String, String> validationErrors = validate(value, groups);
            if (! validationErrors.isEmpty()) {
                validationErrors.entrySet()
                        .stream()
                        .forEach(entry -> helper.add(entry.getKey(), entry.getValue()));
                throw new Exception(helper.toString());
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
        private static <T> Map<String, String> validate(T value, Class<?>... groups) {
            Validator validator = factory.getValidator();
            if (!Objects.isNull(groups) || groups.length > 0) {
                return validator.validate(value, groups).stream()
                        .collect(Collectors.toMap(cv -> cv.getPropertyPath().toString(), ConstraintViolation::getMessage));
            }
            return validator.validate(value).stream()
                    .collect(Collectors.toMap(cv -> cv.getPropertyPath().toString(), ConstraintViolation::getMessage));
        }
    }
}
