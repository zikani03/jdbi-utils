package com.github.zikani03.jdbi;

import org.jdbi.v3.core.argument.Argument;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.core.statement.StatementCustomizer;
import org.slf4j.LoggerFactory;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Optional;

/**
 * Notify in Postgres after an SqlUpdate
 */
public class PostgresNotifyCustomizer implements StatementCustomizer {
    private final String channel;
    private final String binding;
    private final String query;

    /**
     * Template for incrementing counter query
     */
    private static final String QUERY_TEMPLATE_INCR = "NOTIFY %s, '?'";

    /**
     *
     * @param channel - The name of the channel to NOTIFY on
     * @param binding - The name of the binding to get the value from. Used for finding the record to update
     */
    public PostgresNotifyCustomizer(String channel, String binding) {
        this.channel = channel;
        this.binding = binding;
        this.query = createQuery();
    }

    /**
     * Create the query from the template using the specified binding value.
     *
     * @return
     */
    private String createQuery() {
        // NOTE: Since this is using interpolation, there is potential for SQL injection. Somebody fix this.
        return String.format(QUERY_TEMPLATE_INCR, channel);
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
