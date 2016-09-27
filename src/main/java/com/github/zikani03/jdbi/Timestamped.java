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
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.LocalDateTime;
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
@Target({ElementType.PARAMETER})
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
        public SqlStatementCustomizer createForParameter(Annotation annotation, Class<?> sqlObjectType, Method method, Object arg) {
            final String createdField = ((Timestamped) annotation).createdAt(),
                modifiedField = ((Timestamped) annotation).modifiedAt();
            final boolean isNew = ((Timestamped) annotation).value();

            return q -> {
                LocalDateTime now = LocalDateTime.now();
                if (isNew) {
                    q.bind(createdField, java.sql.Timestamp.valueOf(now));
                    updateTimestampField(arg, createdField, now);
                }
                q.bind(modifiedField, java.sql.Timestamp.valueOf(now));
                updateTimestampField(arg, modifiedField, now);
            };
        }

        /**
         * Use Bean Introspection to update the field with a datetime value
         *
         * @param object The instance of the Java Bean
         * @param fieldName The field we want to update
         * @param dateTime The value to use during the update
         */
        private static void updateTimestampField(Object object, String fieldName, LocalDateTime dateTime) {
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

                    Object value = dateTime;
                    if (prop.getPropertyType() == LocalDateTime.class) {
                        // don't change it
                    } else if (prop.getPropertyType() == Timestamp.class) {
                        value = Timestamp.valueOf(dateTime);
                    } else if (prop.getPropertyType() == Integer.class || prop.getPropertyType() == Long.class){
                        value = Timestamp.valueOf(dateTime).toInstant().toEpochMilli();
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
