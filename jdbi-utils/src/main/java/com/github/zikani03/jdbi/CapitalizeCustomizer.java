package com.github.zikani03.jdbi;

import org.jdbi.v3.core.argument.Argument;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.core.statement.StatementCustomizer;
import org.slf4j.LoggerFactory;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Optional;

public class CapitalizeCustomizer implements StatementCustomizer {
    private final String[] bindings;

    public CapitalizeCustomizer(String... bindings) {
        this.bindings = bindings;
    }

    @Override
    public void beforeBinding(PreparedStatement stmt, StatementContext ctx) throws SQLException {
        Arrays.stream(bindings).forEach(binding -> {
            Optional<Argument> bindingVal = ctx.getBinding().findForName(binding, ctx);

            if (! bindingVal.isPresent()) {
                LoggerFactory.getLogger(getClass()).warn("Missing binding '{}'. Cannot capitalize", binding);
                return;
            }
            ctx.getBinding().addNamed(binding, String.valueOf(bindingVal.get()).toUpperCase());
        });
    }
}
