Jdbi3 Utilities
===============

A set of utilities for working with [Jdbi 3](https://github.com/jdbi/jdbi)

> **NOTE**:
> This library is using an alpha version of Jdbi as [Jdbi 3 is still in development](https://github.com/jdbi/jdbi/issues)
> so it's not really recommended to use this in production unless you know what you're doing.

With that said:

## SqlObject Utilities

### Validation

The `@Valid` annotation provides validation for your method parameters in SqlObject methods.
The validation is done via the Hibernate Validator.

The annotation also supports validation groups.

A Simple example:

```java

// Person.java
public class Person {
    @Min(value=1, groups=PersonUpdate.class)
    private int id;

    @NotEmpty
    private String firstName;

    @NotEmpty
    private String lastName;

    @Min(18)
    private int age;

    // ... getters and setters ..

    public interface PersonUpdate {}
}

// Application.java
public class Application {

    public static void main(String... args) {
        PersonDAO dao = dbi.onDemand(PersonDAO.class);

        Person p = new Person();

        // This call will throw an `ValidationException` since there are validation errors
        dao.insert(p);
    }

    @RegisterRowMapperFactory(BeanMapperFactory.class)
    public interface PersonDAO {
        @SqlUpdate("INSERT INTO people(firstName, lastName, age) VALUES (:p.firstName, :p.lastName, :p.age)")
        void insert(@BindBean("p") @Valid Person person);

        @SqlUpdate("UPDATE people firstName=:p.firstName, lastName=:p.lastName, age=:p.age WHERE id=:p.id")
        void update(@BindBean("p") @Valid(groups=Person.PersonUpdate.class) Person person);
    }
}
```

> NOTE: You don't necessarily have to use the `@BindBean` annotation. It should work with any
binding annotation - but that's not been tested thoroughly.

### SQL Logging

Add the `@LogSql` annotation to your SqlObjects to log executed SQL statements, by default the statements
are logged using the `DEBUG` level but you can enforce the `INFO` level as in the example below:

```java
@LogSql(level = LogSql.LogLevel.INFO)
public interface PersonDAO {
    @SqlUpdate("INSERT INTO people(firstName, lastName, age) VALUES (:p.firstName, :p.lastName, :p.age)")
    void insert(@BindBean("p") @Valid Person person);

    @SqlUpdate("UPDATE people firstName=:p.firstName, lastName=:p.lastName, age=:p.age WHERE id=:p.id")
    void update(@BindBean("p") @Valid(groups=Person.PersonUpdate.class) Person person);
}
```

### Timestamp

Add the `@Timestamped` annotation to a parameter in an SqlObject method to get timestamping behavior.<br/>

The annotation binds the named parameters <code>:created</code> and <code>:modified</code> to the SQL Statement by default.

After the method executes the bean will have it's timestamp values updated to the same
value that is sent to the database via the <code>:created</code> and <code>:modified</code> parameters

```java
public interface PersonDAO {
    @SqlUpdate("INSERT INTO people(id, firstName, lastName, email, created, modified) VALUES (:p.id, :p.firstName, :p.lastName, :p.email, :created, :modified)")
    void insert(@BindBean("p") @Timestamped Person person);
}
```

So, if you called the `insert` method somewhere, like `dao.insert(person)` - you would expect the values of
`person.getCreated()` and `person.getModified()` to return updated values accordingly.

## Building, Using

Use Maven 3 to compile and install the library to your local maven repo:

```
$ git clone https://github.com/zikani03/jdbi-utils.git

$ cd jdbi-utils

$ mvn clean install
```

Add as a dependency in your `pom.xml`

```xml
<dependency>
    <groupId>com.github.zikani03</groupId>
    <artifactId>jdbi-utils</artifactId>
    <version>0.1.0</version>
</dependency>
```

## Contributing

Pull requests are most welcome.

## LICENSE

MIT
