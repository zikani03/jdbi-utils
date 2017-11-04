package com.github.zikani03.jdbi;

import org.jdbi.v3.sqlobject.SqlObjectPlugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.validation.ValidationException;
import java.util.Optional;

import static com.github.zikani03.jdbi.Validation.valid;

/**
 * Tests for the Validation statement customizer
 */
public class TestValid {

    public PersonDAO dao;

    public HsqldbDatabaseRule hsql = new HsqldbDatabaseRule();

    @BeforeEach
    public void beforeEach() throws Exception {
        hsql.before();
        hsql.getJdbi().installPlugin(new SqlObjectPlugin());
        dao = hsql.onDemand(PersonDAO.class);
    }

    @AfterEach
    public void  afterEach() throws Exception {
        hsql.after();
    }

    @Test
    public void testThrowOnTryingToInsertNull() {
        Person person = new Person(null, null, null);
        Assertions.assertThrows(ValidationException.class, () -> dao.insert(person));
    }

    @Test
    public void testThrowOnTryingToInsertEmptyFirstName() {
        Person person = new Person("", "Phiri", "phiri@gmail.com");
        Assertions.assertThrows(ValidationException.class, () -> dao.insert(person));
    }

    @Test
    public void testThrowOnTryingToInsertEmptyLastName() {
        Person person = new Person("John", "", "phiri@gmail.com");
        Assertions.assertThrows(ValidationException.class, () -> dao.insert(person));
    }

    @Test
    public void testInsertWithoutValidationErrors() {
        Person person = new Person("John", "Phiri", "phiri@gmail.com");
        person.setId(1);
        dao.insert(person);

        Person p = dao.get(1);

        Assertions.assertTrue(person.equals(p));
    }

    @Test
    public void testValidateGroups() {
        Person person = new Person("John", "Phiri", "phiri@gmail.com");
        person.setId(1);

        dao.insert(person);

        person.setEmail("not an email address");

        Assertions.assertThrows(ValidationException.class, () -> dao.updateEmail(person));
    }


    static String SQL_INSERT = "INSERT INTO people(id, firstName, lastName, email, created, modified) VALUES (:id, :firstName, :lastName, :email, :created, :modified)";
    static String SQL_SELECT = "SELECT id, firstName, lastName, email, created, modified from people WHERE id=:id";

    @Test
    public void testThrowOnTryingToInsertNullWithValidMethod() {
        Person person = new Person(null, null, null);
        Assertions.assertThrows(ValidationException.class, () ->
                hsql.openHandle().createUpdate(SQL_INSERT)
                        .bindBean(valid(person))
                        .execute()
        );
    }

    @Test
    public void testThrowOnTryingToInsertEmptyFirstNameWithValidMethod() {
        Person person = new Person("", "Phiri", "phiri@gmail.com");

        Assertions.assertThrows(ValidationException.class, () ->
                hsql.openHandle().createUpdate(SQL_INSERT)
                        .bindBean(valid(person))
                        .execute()
        );
    }

    @Test
    public void testThrowOnTryingToInsertEmptyLastNameWithValidMethod() {
        Person person = new Person("John", "", "phiri@gmail.com");
        Assertions.assertThrows(ValidationException.class, () ->
                hsql.openHandle().createUpdate(SQL_INSERT)
                        .bindBean(valid(person))
                        .execute()
        );
    }

    @Test
    public void testDoesNotThrowOnValidBean() {
        Person person = new Person("John", "Phiri", "phiri@gmail.com");
        person.setId(1);
        hsql.openHandle().createUpdate(SQL_INSERT)
                .bindBean(valid(person))
                .execute();
        int id = 1;
        Optional<Person> p = hsql.openHandle().createQuery(SQL_SELECT)
                .bind("id", id)
                .registerRowMapper(new PersonDAO.PersonRowMapper())
                .mapTo(Person.class)
                .findFirst();

        Assertions.assertTrue(p.isPresent());
        Assertions.assertTrue(person.equals(p.get()));

    }
}
