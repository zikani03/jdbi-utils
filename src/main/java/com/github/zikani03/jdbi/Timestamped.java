package com.github.zikani03.jdbi;

import org.jdbi.v3.sqlobject.SqlStatementCustomizer;
import org.jdbi.v3.sqlobject.SqlStatementCustomizerFactory;
import org.jdbi.v3.sqlobject.SqlStatementCustomizingAnnotation;
import org.slf4j.LoggerFactory;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.annotation.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Objects;

/**
 * Naive Timestamping for Java Beans(tm).
 *
 * Apply to a parameter in a <b>Jdbi3 SqlObject</b> method to get timestamping behavior.<br/>
 * Binds the named parameters <code>:created</code> and <code>:modified</code> in the SQL by default.
 *
 * <p>
 * After the method executes the bean will have it's timestamp values updated to the same
 * value that is sent to the database via the <code>:created</code> and <code>:modified</code> parameters
 * </p>
 *
 * Example:
 * <pre>
 * public interface PersonDAO {
 *      @GetGeneratedKeys
 *      @SqlUpdate("INSERT INTO people(id, firstName, lastName, email, created, modified) VALUES (:p.id, :p.firstName, :p.lastName, :p.email, :created, :modified)")
 *      int insert(@BindBean("p") @Valid @Timestamped Person person);
 *  }
 * </pre>
 *
 * @author zikani
 */
@Target({ElementType.METHOD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@SqlStatementCustomizingAnnotation(Timestamped.Factory.class)
@Documented
public @interface Timestamped {
    /**
     * Whether or not the argument represents a "new" record or an update
     * @return
     */
    boolean value() default true;

    String createdAt() default "created";

    String modifiedAt() default "modified";

    class Factory implements SqlStatementCustomizerFactory {

        @Override
        public SqlStatementCustomizer createForMethod(Annotation annotation, Class<?> sqlObjectType, Method method) {
            final String createdField = ((Timestamped) annotation).createdAt(),
                    modifiedField = ((Timestamped) annotation).modifiedAt();
            final boolean isNew = ((Timestamped) annotation).value();

            return q -> {
                OffsetDateTime now = OffsetDateTime.now();
                if (isNew) {
                    q.bind(createdField, now);
                }
                q.bind(modifiedField, now);
            };
        }

        @Override
        public SqlStatementCustomizer createForParameter(Annotation annotation, Class<?> sqlObjectType, Method method, Parameter param, Object arg) {
            final String createdField = ((Timestamped) annotation).createdAt(),
                modifiedField = ((Timestamped) annotation).modifiedAt();
            final boolean isNew = ((Timestamped) annotation).value();

            return q -> {
                OffsetDateTime now = OffsetDateTime.now();
                if (isNew) {
                    q.bind(createdField, now);
                    updateTimestampField(arg, createdField, now);
                }
                q.bind(modifiedField, now);
                updateTimestampField(arg, modifiedField, now);
            };
        }

        /**
         * Use Bean Introspection to update the field with a datetime value
         *
         * @param object The instance of the Java Bean
         * @param fieldName The field we want to update
         * @param dateTime The value to use during the update - using OffsetDateTime to account for different timezone/locale settings
         */
        private static void updateTimestampField(Object object, String fieldName, OffsetDateTime dateTime) {
            try {
                BeanInfo infos = Introspector.getBeanInfo(object.getClass());
                PropertyDescriptor[] props = infos.getPropertyDescriptors();

                for (PropertyDescriptor prop : props) {
                    if (Objects.equals("class", prop.getName())) {
                        continue;
                    }

                    // Skip fields we are not interested in
                    if (! Objects.equals(fieldName, prop.getName())) {
                        continue;
                    }

                    Object value = null;
                    if (prop.getPropertyType() == OffsetDateTime.class) {
                        value = dateTime;
                    } else if (prop.getPropertyType() == LocalDateTime.class) {
                        value = dateTime.toLocalDateTime();
                    } else if (prop.getPropertyType() == Timestamp.class) {
                        value = Timestamp.from(dateTime.toInstant());
                    } else if (prop.getPropertyType() == Integer.class || prop.getPropertyType() == Long.class){
                        value = dateTime.toInstant().toEpochMilli();
                    } else if (prop.getPropertyType() == String.class) {
                        value = dateTime.toString();
                    } else {
                        throw new IllegalArgumentException(String.format("Cannot cast %s to %s", dateTime, prop.getPropertyType().getName()));
                    }

                    Method writeMethod = prop.getWriteMethod();
                    if (writeMethod != null) {
                        writeMethod.invoke(object, value);
                    }
                }
            } catch (IntrospectionException | InvocationTargetException | IllegalAccessException e) {
                LoggerFactory.getLogger(Timestamped.class).error("Failed to set field: {}", fieldName);
            }
        }
    }
}
