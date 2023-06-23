package com.maven.maven;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.VariableDeclarator;
import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

@Mojo(
        name = "check",
        defaultPhase = LifecyclePhase.VALIDATE,
        requiresDependencyResolution = ResolutionScope.COMPILE
)
public class StyleCheckMojo extends AbstractMojo {

    private static final Pattern SNAKE_CASE_PATTERN = Pattern.compile("^[a-z][a-z0-9_]*$");
    private static final Pattern CAMEL_CASE_PATTERN = Pattern.compile("^[a-z][a-zA-Z0-9]*$");
    private static final Pattern PASCAL_CASE_PATTERN = Pattern.compile("^[A-Z][a-zA-Z0-9]*$");

    public void execute() throws MojoExecutionException, MojoFailureException {
        File basedir = new File("").getAbsoluteFile();
        Collection<File> javaFiles = FileUtils.listFiles(basedir, new String[]{"java"}, true);

        for (File javaFile : javaFiles) {
            checkFile(javaFile);
        }
    }

    private void checkFile(File javaFile) throws MojoExecutionException {
        try {
            CompilationUnit cu = StaticJavaParser.parse(javaFile);
            String className = getClassName(cu);

            checkMethods(cu, className);
            checkVariables(cu, className);
            checkClassNames(className);
        } catch (FileNotFoundException e) {
            throw new MojoExecutionException("Failed to parse Java file: " + javaFile.getAbsolutePath(), e);
        }
    }

    private void checkMethods(CompilationUnit cu, String className) throws MojoExecutionException {
        List<MethodDeclaration> methodDeclarations = cu.findAll(MethodDeclaration.class);
        for (MethodDeclaration methodDeclaration : methodDeclarations) {
            String methodName = methodDeclaration.getNameAsString();
            if (!isSnakeCase(methodName)) {
                String message = String.format("Code style violated in method '%s' of class '%s'. Method names should only use snake_case.", methodName, className);
                throw new MojoExecutionException(message);
            }
        }
    }

    private void checkVariables(CompilationUnit cu, String className) throws MojoExecutionException {
        List<VariableDeclarator> variableDeclarators = cu.findAll(VariableDeclarator.class);
        for (VariableDeclarator variableDeclarator : variableDeclarators) {
            String variableName = variableDeclarator.getNameAsString();
            if (!isCamelCase(variableName)) {
                String message = String.format("Code style violated in variable '%s' of class '%s'. Variable names should only use camelCase.", variableName, className);
                throw new MojoExecutionException(message);
            }
        }

        List<Parameter> parameters = cu.findAll(Parameter.class);
        for (Parameter parameter : parameters) {
            String parameterName = parameter.getNameAsString();
            if (!isCamelCase(parameterName)) {
                String message = String.format("Code style violated in parameter '%s' of class '%s'. Parameter names should only use camelCase.", parameterName, className);
                throw new MojoExecutionException(message);
            }
        }
    }

    private void checkClassNames(String className) throws MojoExecutionException {
        if (!isPascalCase(className)) {
            String message = String.format("Code style violated in class '%s'. Class names should only use PascalCase.", className);
            throw new MojoExecutionException(message);
        }
    }

    private String getClassName(CompilationUnit cu) {
        List<ClassOrInterfaceDeclaration> classDeclarations = cu.findAll(ClassOrInterfaceDeclaration.class);
        if (!classDeclarations.isEmpty()) {
            ClassOrInterfaceDeclaration classDeclaration = classDeclarations.get(0);
            return classDeclaration.getNameAsString();
        }
        return "";
    }

    private boolean isSnakeCase(String name) {
        return SNAKE_CASE_PATTERN.matcher(name).matches();
    }

    private boolean isCamelCase(String name) {
        return CAMEL_CASE_PATTERN.matcher(name).matches();
    }

    private boolean isPascalCase(String name) {
        return PASCAL_CASE_PATTERN.matcher(name).matches();
    }
}
