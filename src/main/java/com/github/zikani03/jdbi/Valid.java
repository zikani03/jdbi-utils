package com.github.zikani03.jdbi;

import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.core.statement.StatementCustomizer;
import org.jdbi.v3.sqlobject.customizer.SqlStatementCustomizer;
import org.jdbi.v3.sqlobject.customizer.SqlStatementCustomizerFactory;
import org.jdbi.v3.sqlobject.customizer.SqlStatementCustomizingAnnotation;
import org.jdbi.v3.sqlobject.customizer.SqlStatementParameterCustomizer;

import java.lang.annotation.*;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.sql.PreparedStatement;
import java.sql.SQLException;

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
        public SqlStatementParameterCustomizer createForParameter(Annotation annotation, Class<?> sqlObjectType, Method method, Parameter param, int index) {
            final Class<?>[] validationGroups = ((Valid) annotation).groups();
            return create(validationGroups);
        }

        private SqlStatementParameterCustomizer create(Class<?>... groups) {
            return (q, entity) -> q.addCustomizer(new ValidatingCustomizer(entity, groups));
        }
    }

    /**
     * Statement Customizer that validates method parameters
     *
     */
    class ValidatingCustomizer implements StatementCustomizer {
        final Object entity;
        final Class<?>[] groups;

        public ValidatingCustomizer(Object arg, Class<?>[] groups) {
            this.entity = arg;
            this.groups = groups;
        }

        @Override
        public void beforeExecution(PreparedStatement stmt, StatementContext ctx) throws SQLException {
            Validation.throwOnFailedValidation(entity, groups);
        }
    }
}
