package com.github.zikani03.jdbi;

import org.jdbi.v3.core.ConnectionFactory;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.spi.JdbiPlugin;
import org.junit.rules.ExternalResource;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * H2DatabaseRule
 */
public class HsqldbDatabaseRule extends ExternalResource {
    private final String uri = "jdbc:hsqldb:mem:" + UUID.randomUUID();
    private Connection con;
    private Jdbi dbi;
    private Handle sharedHandle;
    private boolean installPlugins = false;
    private List<JdbiPlugin> plugins = new ArrayList<>();

    @Override
    protected void before() throws Throwable
    {
        dbi = Jdbi.create(uri);
        if (installPlugins) {
            dbi.installPlugins();
        }
        plugins.forEach(dbi::installPlugin);
        sharedHandle = dbi.open();
        con = sharedHandle.getConnection();
        try (Statement s = con.createStatement()) {
            s.execute("create table people(id identity primary key, firstName varchar(50), lastName varchar(50), email varchar(255), created timestamp, modified timestamp);");
        }
    }

    @Override
    protected void after()
    {
        try {
            con.close();
        } catch (SQLException e) {
            throw new AssertionError(e);
        }
    }

    public HsqldbDatabaseRule withPlugins()
    {
        installPlugins = true;
        return this;
    }

    public HsqldbDatabaseRule withPlugin(JdbiPlugin plugin)
    {
        plugins.add(plugin);
        return this;
    }

    public String getConnectionString()
    {
        return uri;
    }

    public Jdbi getJdbi()
    {
        return dbi;
    }

    public Handle getSharedHandle()
    {
        return sharedHandle;
    }

    public Handle openHandle()
    {
        return getJdbi().open();
    }

    public <T> T onDemand(Class<T> clazz) {
        return dbi.onDemand(clazz);
    }

    public ConnectionFactory getConnectionFactory()
    {
        return () -> DriverManager.getConnection(getConnectionString());
    }
}
