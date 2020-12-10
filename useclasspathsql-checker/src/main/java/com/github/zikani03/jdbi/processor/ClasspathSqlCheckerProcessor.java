package com.github.zikani03.jdbi.processor;

import org.jdbi.v3.sqlobject.locator.UseClasspathSqlLocator;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * Annotation processor for the paranoid.
 * Checks if an SQL File exists for an SqlObject annotated with
 * {@link UseClasspathSqlLocator}
 */
public class ClasspathSqlCheckerProcessor extends AbstractProcessor {
    // private Types typeUtils;
    private Elements elementUtils;
    private Filer filer;
    private Messager messager;

    @SuppressWarnings("unused")
    public ClasspathSqlCheckerProcessor() {
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        Set<String> annotations = new HashSet<>();
        annotations.add(UseClasspathSqlLocator.class.getCanonicalName());
        return annotations;
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public synchronized void init(ProcessingEnvironment env) {
        super.init(env);
        // typeUtils = env.getTypeUtils();
        elementUtils = env.getElementUtils();
        filer = env.getFiler();
        messager = env.getMessager();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment env) {
        for(Element annotatedElement : env.getElementsAnnotatedWith(UseClasspathSqlLocator.class)) {
            messager.printMessage(Diagnostic.Kind.NOTE, "Processing class: " + annotatedElement.getSimpleName());
            if (!annotatedElement.getKind().equals(ElementKind.INTERFACE) || !annotatedElement.getKind().equals(ElementKind.CLASS)) {
                messager.printMessage(Diagnostic.Kind.ERROR,
                        String.format("Class annotated with UseClassPathSqlLocator is not an interface: %s Kind: %s",
                            annotatedElement.getSimpleName(),
                            annotatedElement.getKind()),
                        annotatedElement);
                env.errorRaised();
                return true;
            }
            TypeElement classElement = (TypeElement) annotatedElement;

            for(Element member: elementUtils.getAllMembers(classElement)) {
                if (member.getAnnotation(SqlQuery.class) == null ||
                    member.getAnnotation(SqlUpdate.class) == null) {
                    // member.getKind() != ElementKind.METHOD ||
                    continue;
                }

                Name methodName = member.getSimpleName();
                String packageName = elementUtils.getPackageOf(classElement).toString();

                String sqlFilename = annotatedElement.getSimpleName() + "/" + methodName.toString() + ".sql";

                try {
                    FileObject f = filer.getResource(StandardLocation.CLASS_PATH, packageName, sqlFilename);
                    CharSequence data = f.getCharContent(true);
                    if (data == null || data.length() < 1) {
                        messager.printMessage(Diagnostic.Kind.ERROR,
                            String.format("ClasspathSqlChecker found error: SQL File '%s' is empty", sqlFilename),
                            annotatedElement
                        );
                        env.errorRaised();
                        break;
                    }
                } catch(IOException ioe) {
                    messager.printMessage(Diagnostic.Kind.ERROR,
                        String.format("ClasspathSqlChecker failed to load %s", sqlFilename),
                        annotatedElement
                    );
                    env.errorRaised();
                    break;
                }
            }
        }
        return false;
    }
}
