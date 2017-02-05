package com.github.zikani03.jdbi;

import org.jdbi.v3.sqlobject.customizer.SqlStatementCustomizer;
import org.jdbi.v3.sqlobject.customizer.SqlStatementCustomizerFactory;
import org.jdbi.v3.sqlobject.customizer.SqlStatementCustomizingAnnotation;
import org.jdbi.v3.sqlobject.customizer.SqlStatementParameterCustomizer;

import java.lang.annotation.*;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

/**
 * Executes a statement to update a counter field in a table after executing a query on an SqlObject method
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@SqlStatementCustomizingAnnotation(Counter.Factory.class)
public @interface Counter {

    /**
     * The table containing the counter field
     */
    String table();

    /**
     * The name of the counter field column. Should be and integer type
     */
    String column();

    /**
     * The name of the binding to get the value from. Used for finding the record to update
     */
    String binding();

    /**
     * The name of the primary key column to use for finding the record to update. Defaults to "id"
     */
    String primaryKey() default "id";

    /**
     * Whether the counter should be a decrementing counter or not.
     * Defaults to false to make it an incrementing counter
     *
     * @return
     */
    boolean decrementing() default false;

    class Factory implements SqlStatementCustomizerFactory {

        @Override
        public SqlStatementCustomizer createForMethod(Annotation annotation, Class<?> sqlObjectType, Method method) {
            return counter((Counter) annotation);
        }

        @Override
        public SqlStatementParameterCustomizer createForParameter(Annotation annotation, Class<?> sqlObjectType, Method method, Parameter param, int index) {
            return (stmt, arg) -> counter((Counter) annotation);
        }

        private SqlStatementCustomizer counter(Counter config) {
            final String table = config.table(),
                    column = config.column(),
                    binding = config.binding(),
                    primaryKey = config.primaryKey();
            final boolean decrementing = config.decrementing();
            return (stmt) -> stmt.addCustomizer(new CounterCustomizer(table, column, binding, primaryKey, decrementing));
        }
    }
}