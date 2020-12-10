package com.github.zikani03.jdbi;

import org.jdbi.v3.core.argument.Argument;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.core.statement.StatementCustomizer;
import org.slf4j.LoggerFactory;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Optional;

/**
 * CounterCustomizer - Executes a statement to update a counter field
 */
public class CounterCustomizer implements StatementCustomizer {
    private final String table;
    private final String column;
    private final String binding;
    private final String primaryKey;
    private final String query;
    private final boolean isDecrementing;

    /**
     * Template for incrementing counter query
     */
    private static final String QUERY_TEMPLATE_INCR = "UPDATE %s SET %s = %s + 1 WHERE %s =  ?";

    /**
     * Template for decrementing counter query
     */
    private static final String QUERY_TEMPLATE_DECR = "UPDATE %s SET %s = %s - 1 WHERE %s =  ?";

    /**
     *
     * @param table - The table containing the counter field
     * @param column - The name of the counter field column. Should be and integer type
     * @param binding - The name of the binding to get the value from. Used for finding the record to update
     * @param primaryKey - the name of the primary key column to use for finding the record to update
     */
    public CounterCustomizer(String table, String column, String binding, String primaryKey) {
        this.table = table;
        this.column = column;
        this.binding = binding;
        this.primaryKey = primaryKey;
        isDecrementing = false;
        this.query = createQuery();
    }
    /**
     *
     * @param table - The table containing the counter field
     * @param column - The name of the counter field column. Should be and integer type
     * @param binding - The name of the binding to get the value from. Used for finding the record to update
     * @param primaryKey - the name of the primary key column to use for finding the record to update
     * @param isDecrementing - If the counter should be a decrementing counter
     */
    public CounterCustomizer(String table, String column, String binding, String primaryKey, boolean isDecrementing) {
        this.table = table;
        this.column = column;
        this.binding = binding;
        this.primaryKey = primaryKey;
        this.isDecrementing = isDecrementing;
        this.query = createQuery();
    }

    /**
     * Create the query from the template using the specified binding value.
     *
     * @return
     */
    private String createQuery() {
        // NOTE: Since this is using interpolation, there is potential for SQL injection. Somebody fix this.
        return String.format(isDecrementing ? QUERY_TEMPLATE_DECR : QUERY_TEMPLATE_INCR,
                             table,
                             column,
                             column,
                             primaryKey);
    }

    @Override
    public void afterExecution(PreparedStatement stmt, StatementContext ctx) throws SQLException {
        Optional<Argument> bindingVal = ctx.getBinding().findForName(binding, ctx);

        if (! bindingVal.isPresent()) {
            LoggerFactory.getLogger(getClass()).warn("Missing binding '{}'. Cannot update counter", binding);
            return;
        }

        try (PreparedStatement sql = ctx.getConnection().prepareStatement(query)) {
            sql.setString(1, String.valueOf(bindingVal.get()));
            sql.execute();
            LoggerFactory.getLogger(getClass()).debug("Executed SQL: {}", query);
        } catch (SQLException e) {
            throw e;
        }
    }
}
