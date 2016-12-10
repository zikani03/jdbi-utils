package com.github.zikani03.jdbi;

import org.jdbi.v3.sqlobject.SqlObjectPlugin;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.gen5.api.Assertions;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

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
        p.setId(1);
        dao.insert(p);

        Person found = dao.get(1);

        Assertions.assertNotNull(found.getCreated());
        Assertions.assertNotNull(found.getModified());

        // This is one way we can get the binding information of the executed query
        hsql.getJdbi().setTimingCollector((l, statementContext) -> {
            Assertions.assertTrue(statementContext.getBinding().findForName("now").isPresent());
        });
    }

    @Test
    public void shouldAllowCustomTimestampParameter() {
        LocalDateTime timeBefore = LocalDateTime.now();
        Person p = new Person("John", "Phiri", "");
        p.setId(1);
        PersonDAO personDao = hsql.onDemand(PersonDAO.class);

        personDao.insertWithCustomTimestampFields(p);

        Person fetched = dao.get(1);

        Assertions.assertEquals(p.getFirstName(), fetched.getFirstName());
        Assertions.assertEquals(p.getLastName(), fetched.getLastName());
        Assertions.assertNotNull(fetched.getCreated());
        Assertions.assertNotNull(fetched.getModified());

        Assertions.assertTrue(timeBefore.isBefore(fetched.getCreated().toLocalDateTime()));
        Assertions.assertTrue(timeBefore.isBefore(fetched.getModified().toLocalDateTime()));

        // Ensure our custom fields were bound properly
        hsql.getJdbi().setTimingCollector((l, statementContext) -> {
            Assertions.assertTrue(statementContext.getBinding().findForName("createdAt").isPresent());
        });
    }

    @Test
    public void shouldUpdateModifiedTimestamp() {
        PersonDAO personDao = hsql.onDemand(PersonDAO.class);

        Person p = new Person("John", "Phiri", "");

        p.setId(3);

        personDao.insert(p);

        Person personAfterCreate = dao.get(3);

        personAfterCreate.setLastName("Banda");

        personDao.updatePerson(personAfterCreate);

        Person personAfterUpdate = dao.get(3);

        assertThat(personAfterUpdate.getLastName()).isEqualToIgnoringCase("Banda");

        assertThat(personAfterUpdate.getCreated()).isEqualTo(personAfterCreate.getCreated());

        assertThat(personAfterUpdate.getModified()).isAfter(personAfterCreate.getModified());

        // Ensure our custom fields were bound properly
        hsql.getJdbi().setTimingCollector((l, statementContext) -> {
            Assertions.assertTrue(statementContext.getBinding().findForName("now").isPresent());
        });
    }
}
