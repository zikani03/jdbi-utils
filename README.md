Jdbi3 Utilities
===============

A set of utilities for working with [Jdbi 3](https://github.com/jdbi/jdbi)

### Counter

Use the `CounterCustomizer` to automatically update a "counter field" in a table.

For example if you wanted to update a user's post count after inserting a new Post
you would do something like this:

```java
Jdbi.open()
    .createUpdate("INSERT INTO posts(content, user_id) VALUES (:content, :user_id)")
    .bind("content", "Yay! Post content!")
    .bind("user_id", 1)
    .addCustomizer(new CounterCustomizer("users", "posts_count", "user_id", "id"))
    .execute();
```

This will increment the `posts_count` column in the users table automatically!

> NOTE: In order to avoid potential SQL injections, you **MUST NOT** use _untrusted_ user input as arguments for the `CounterCustomizer` constructor

You can even use it with SqlObjects using the `@Counter` annotation :

```java
public interface PostDAO {
    @SqlUpdate("INSERT INTO posts(content, user_id) VALUES (:p.content, :p.userId)")
    @Counter(table = "users", column = "posts_count", binding = "p.userId")
    void insert(@BindBean("p") Post post);
}
```

You can also make the counter decrement by setting the `decrementing` argument to `true` in the annotation.

### Capitalize Customizer

Use the `CapitalizeCustomizer` to automatically set bound fields to UPPER CASE!

For example if you wanted to capitalize the content of each Post before saving
to the database you would do something like this:

```java
Jdbi.open()
    .createUpdate("INSERT INTO posts(content, user_id) VALUES (:content, :user_id)")
    .bind("content", "Yay! Post content!")
    .bind("user_id", 1)
    .addCustomizer(new CapitalizeCustomizer("content"))
    .execute();
```


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

### @UseClasspathSqlLocator compile-time checks

The `useclasspathsql-checker` module implements an Annotation Processor that checks that 
SQL files for methods on your DAOs (SqlObjects in JDBI speak) exist and are not empty. 

In particular, it checks for SqlObjects that use the `@UseClasspathSqlLocator`
feature. It checks for SQL files for methods annotated with `@SqlQuery` or `@SqlUpdate`.

**Why is this important?**

Using the annotation processor will protect you from bugs caused by a 
missing or empty `.sql` file - **at compile time**. 

JDBI does throw Exceptions for these problems at runtime but it could be too late at that point, you know.

**Usage**

Just add the following dependency to your project, note the scope is `provided`

```xml
<dependency>
    <groupId>com.github.zikani03</groupId>
    <artifactId>jdbi-utils-useclasspathsql-checker</artifactId>
    <version>0.4.0</version>
    <scope>provided</scope>
</dependency>
```

This will generate an error like the following if an SQL file cannot be found:

```
[ERROR] .../jdbi-utils/example/src/main/java/com/github/zikani03/jdbi/ExampleDAO.java:[7,8] ClasspathSqlChecker could not find or load SQL file: ExampleDAO/selectOne.sql
```

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

## Installation

You can get the library via [JitPack](https://jitpack.io). First of all add the following repository to your
build file

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>
```

After that, you can add the library as a dependency in your project's `pom.xml`

```xml
<dependency>
    <groupId>com.github.zikani03</groupId>
    <artifactId>jdbi-utils</artifactId>
    <version>0.4.0</version>
</dependency>
```

Alternatively, clone the git repository and install to your local maven repo with `mvn clean install`

## Contributing

Pull requests are most welcome.

## LICENSE

MIT
