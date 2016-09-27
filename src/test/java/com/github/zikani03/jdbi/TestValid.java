package com.github.zikani03.jdbi;

import org.jdbi.v3.sqlobject.SqlObjectPlugin;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.gen5.api.Assertions;

/**
 * Tests for the Validation statement customizer
 */
public class TestValid {

    public PersonDAO dao;

    @Rule
    public HsqldbDatabaseRule hsql = new HsqldbDatabaseRule();

    @Before
    public void beforeEach() {
        hsql.getJdbi().installPlugin(new SqlObjectPlugin());
        dao = hsql.onDemand(PersonDAO.class);
    }

    @Test
    public void testThrowOnTryingToInsertNull() {
        Person person = new Person(null, null, null);
        Assertions.assertThrows(IllegalArgumentException.class, () -> dao.insert(person));
    }

    @Test
    public void testThrowOnTryingToInsertEmptyFirstName() {
        Person person = new Person("", "Phiri", "phiri@gmail.com");
        Assertions.assertThrows(IllegalArgumentException.class, () -> dao.insert(person));
    }

    @Test
    public void testThrowOnTryingToInsertEmptyLastName() {
        Person person = new Person("John", "", "phiri@gmail.com");
        Assertions.assertThrows(IllegalArgumentException.class, () -> dao.insert(person));
    }

    @Test
    public void testInsertWithoutValidationErrors() {
        Person person = new Person("John", "Phiri", "phiri@gmail.com");
        dao.insert(person);
    }

    @Test
    public void testValidateGroups() {
        Person person = new Person("John", "Phiri", "phiri@gmail.com");
        person.setId(1);

        dao.insert(person);

        person.setEmail("not an email address");

        Assertions.assertThrows(IllegalArgumentException.class, () -> dao.updateEmail(person));
    }
}
