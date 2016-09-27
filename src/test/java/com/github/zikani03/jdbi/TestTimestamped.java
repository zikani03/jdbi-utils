package com.github.zikani03.jdbi;

import org.jdbi.v3.sqlobject.BindBean;
import org.jdbi.v3.sqlobject.GetGeneratedKeys;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;
import org.jdbi.v3.sqlobject.SqlUpdate;
import org.jdbi.v3.sqlobject.customizers.RegisterRowMapper;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.gen5.api.Assertions;
import org.slf4j.LoggerFactory;

/**
 * Tests for the Timestamped StatementCustomizer
 *
 */
public class TestTimestamped {
    public PersonDAO dao;

    @Rule
    public HsqldbDatabaseRule hsql = new HsqldbDatabaseRule();

    @Before
    public void beforeEach() {
        hsql.getJdbi().installPlugin(new SqlObjectPlugin());
        dao = hsql.onDemand(PersonDAO.class);
    }

    @Test
    public void shouldInsertCreatedAndModifiedFields() {
        Person p = new Person("John", "Phiri", "");

        dao.insert(p);

        Assertions.assertNotNull(p.getCreated());
        Assertions.assertNotNull(p.getModified());

        // This is one way we can get the binding information of the executed query,
        // I do not know any other way yet :D
        hsql.getJdbi().setTimingCollector((l, statementContext) -> {
            Assertions.assertTrue(statementContext.getBinding().findForName("created").isPresent());
            Assertions.assertTrue(statementContext.getBinding().findForName("modified").isPresent());
        });
    }

    @Test
    public void shouldInsertWithCustomCreatedAndModifiedFields() {
        Person p = new Person("John", "Phiri", "");
        p.setId(1);
        CustomTimestampFieldsDAO customDAO = hsql.onDemand(CustomTimestampFieldsDAO.class);

        customDAO.insertWithCustomTimestampFields(p);

        Assertions.assertNull(p.getCreated());
        Assertions.assertNull(p.getModified());

        // Ensure our custom fields were bound properly
        hsql.getJdbi().setTimingCollector((l, statementContext) -> {
            Assertions.assertTrue(statementContext.getBinding().findForName("createdAt").isPresent());
            Assertions.assertTrue(statementContext.getBinding().findForName("modifiedAt").isPresent());
        });
    }

    @Test
    public void shouldInsertWithCustomCreatedAndModifiedFieldsButStillMapToBeanFields() {
        Person p = new Person("John", "Phiri", "");
        p.setId(1);
        CustomTimestampFieldsDAO customDAO = hsql.onDemand(CustomTimestampFieldsDAO.class);

        customDAO.insertWithCustomTimestampFields(p);

        Person fetched = dao.get(1);

        Assertions.assertEquals(p.getFirstName(), fetched.getFirstName());
        Assertions.assertEquals(p.getLastName(), fetched.getLastName());
        Assertions.assertNotNull(fetched.getCreated());
        Assertions.assertNotNull(fetched.getModified());

        // Ensure our custom fields were bound properly
        hsql.getJdbi().setTimingCollector((l, statementContext) -> {
            Assertions.assertTrue(statementContext.getBinding().findForName("createdAt").isPresent());
            Assertions.assertTrue(statementContext.getBinding().findForName("modifiedAt").isPresent());
        });
    }

    @Test
    public void shouldUpdateModifiedField() {
        Person p = new Person("John", "Phiri", "");
        p.setId(1);
        OnlyUpdatesModifiedField updateDAO = hsql.onDemand(OnlyUpdatesModifiedField.class);

        updateDAO.insert(p);
        Person p2 = dao.get(1);

        Assertions.assertNull(p2.getCreated());
        Assertions.assertNotNull(p2.getModified());
    }

    @RegisterRowMapper(PersonDAO.PersonRowMapper.class)
    public interface CustomTimestampFieldsDAO {
        @SqlUpdate("INSERT INTO people(id, firstName, lastName, email, created, modified) VALUES (:p.id, :p.firstName, :p.lastName, :p.email, :createdAt, :modifiedAt)")
        int insertWithCustomTimestampFields(@BindBean("p") @Timestamped(createdAt = "createdAt", modifiedAt = "modifiedAt") Person person);
    }

    @RegisterRowMapper(PersonDAO.PersonRowMapper.class)
    public interface OnlyUpdatesModifiedField {
        @GetGeneratedKeys
        @SqlUpdate("INSERT INTO people(id, firstName, lastName, email, modified) VALUES (:p.id, :p.firstName, :p.lastName, :p.email, :modified)")
        int insert(@BindBean("p") @Valid @Timestamped(false) Person person);
}
}
