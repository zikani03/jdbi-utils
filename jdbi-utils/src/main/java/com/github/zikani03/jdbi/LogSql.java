package com.github.zikani03.jdbi;

import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.core.statement.StatementCustomizer;
import org.jdbi.v3.sqlobject.customizer.SqlStatementCustomizer;
import org.jdbi.v3.sqlobject.customizer.SqlStatementCustomizerFactory;
import org.jdbi.v3.sqlobject.customizer.SqlStatementCustomizingAnnotation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.*;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Annotate SqlObject classes with this annotation to log executed SQL statements.
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@SqlStatementCustomizingAnnotation(LogSql.Factory.class)
public @interface LogSql {
    enum LogLevel {
        DEBUG,
        INFO
    }
    /**
     * Whether to log the raw SQL or rewritten SQL. Defaults to <code>false</code>
     *
     * @return true to log raw sql and false for rewritten sql
     */
    boolean value() default false;

    LogLevel level() default LogLevel.DEBUG;

    class Factory implements SqlStatementCustomizerFactory {
        @Override
        public SqlStatementCustomizer createForType(Annotation annotation, Class<?> sqlObjectType) {
            boolean logRawSql = ((LogSql) annotation).value();
            LogLevel level = ((LogSql) annotation).level();
            return q -> q.addCustomizer(new SqlLogger(sqlObjectType, logRawSql, level));
        }
    }

    /**
     * Logs SQL statements after execution
     */
    final class SqlLogger implements StatementCustomizer {
        final Logger logger;
        final boolean logRawSql;
        final LogLevel logLevel;

        public SqlLogger(Class<?> sqlObjectType, boolean logRawSql, LogLevel logLevel) {
            this.logger = LoggerFactory.getLogger(sqlObjectType);
            this.logRawSql = logRawSql;
            this.logLevel = logLevel;
        }

        @Override
        public void afterExecution(PreparedStatement stmt, StatementContext ctx) throws SQLException {
            if (logLevel.equals(LogLevel.INFO)) {
                logger.info(logRawSql ? ctx.getRawSql() : ctx.getRenderedSql());
            } else {
                logger.debug(logRawSql ? ctx.getRawSql() : ctx.getRenderedSql());
            }
        }
    }
}
