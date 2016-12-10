package com.github.zikani03.jdbi;

import org.jdbi.v3.sqlobject.SqlStatementCustomizer;
import org.jdbi.v3.sqlobject.SqlStatementCustomizerFactory;
import org.jdbi.v3.sqlobject.SqlStatementCustomizingAnnotation;

import java.lang.annotation.*;
import java.lang.reflect.Method;
import java.time.OffsetDateTime;

/**
 * Apply to an <b>Jdbi3 SqlObject</b> method to get timestamping behavior.<br/>
 * Binds the named parameter <code>:now</code> or a custom named parameter with
 * the current datetime as an {@link OffsetDateTime} in the SQL query.
 *
 * Example:
 * <pre>
 * public interface PersonDAO {
 *      @GetGeneratedKeys
 *      @Timestamped
 *      @SqlUpdate("INSERT INTO people(id, firstName, lastName, email, created, modified) VALUES (:p.id, :p.firstName, :p.lastName, :p.email, :now, :now)")
 *      int insert(@BindBean("p") Person person);
 *  }
 * </pre>
 *
 * @author zikani
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@SqlStatementCustomizingAnnotation(Timestamped.Factory.class)
@Documented
public @interface Timestamped {
    /**
     * The parameter to bind in the SQL query
     *
     * @return
     */
    String value() default "now";

    class Factory implements SqlStatementCustomizerFactory {

        @Override
        public SqlStatementCustomizer createForMethod(Annotation annotation, Class<?> sqlObjectType, Method method) {
            final String parameterName = ((Timestamped) annotation).value();

            return q -> {
                q.bind(parameterName, OffsetDateTime.now());
            };
        }
    }
}
