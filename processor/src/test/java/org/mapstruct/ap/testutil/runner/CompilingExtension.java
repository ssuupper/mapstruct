package org.mapstruct.ap.testutil.runner;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.commons.support.AnnotationSupport;
import org.mapstruct.ap.testutil.WithClasses;
import org.mapstruct.ap.testutil.compilation.model.CompilationOutcomeDescriptor;

import static org.mapstruct.ap.testutil.runner.CompilingStatement.TARGET_COMPILATION_TESTS;
import static org.mapstruct.ap.testutil.runner.CompilingStatement.deleteDirectory;
import static org.mapstruct.ap.testutil.runner.CompilingStatement.getBasePath;
import static org.mapstruct.ap.testutil.runner.InnerAnnotationProcessorRunner.replaceContextClassLoader;

/**
 * @author Filip Hrisafov
 */
public abstract class CompilingExtension implements BeforeEachCallback {

    static final ExtensionContext.Namespace ANNOTATION = ExtensionContext.Namespace.create( new Object() );

    private String classOutputDir;
    private String sourceOutputDir;
    private String additionalCompilerClasspath;
    private final Compiler compiler;

    protected CompilingExtension(Compiler compiler) {
        this.compiler = compiler;
    }

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        ExtensionContext.Store annotationStore = context.getStore( ANNOTATION );
        CompilationCache compilationCache = annotationStore
            .getOrComputeIfAbsent( compiler.name() + "-compilationCache", new Function<String, CompilationCache>() {

                @Override
                public CompilationCache apply(String s) {
                    return new CompilationCache();
                }
            }, CompilationCache.class );

        Method testMethod = context.getRequiredTestMethod();
        Class<?> testClass = context.getRequiredTestClass();
        CompilationRequest compilationRequest = new CompilationRequest(
            getTestClasses( testMethod, testClass ),
            new HashMap<Class<?>, Class<?>>(),
            new ArrayList<String>()
        );

        if ( needsRecompilation( compilationRequest, compilationCache ) ) {
            replaceContextClassLoader( testClass );
        }

        compile( testMethod, testClass, compilationRequest, compilationCache );
    }

    private CompilationOutcomeDescriptor compile(Method testMethod, Class<?> testClass,
        CompilationRequest compilationRequest,
        CompilationCache compilationCache)
        throws Exception {

        if ( !needsRecompilation( compilationRequest, compilationCache ) ) {
            return compilationCache.getLastResult();
        }

        setupDirectories( testMethod, testClass );
        compilationCache.setLastSourceOutputDir( sourceOutputDir );

        boolean needsAdditionalCompilerClasspath = false; // prepareServices();
        CompilationOutcomeDescriptor resultHolder;

        resultHolder = compileWithSpecificCompiler(
            compilationRequest,
            sourceOutputDir,
            classOutputDir,
            needsAdditionalCompilerClasspath ? additionalCompilerClasspath : null
        );

        compilationCache.update( compilationRequest, resultHolder );
        return resultHolder;
    }

    protected abstract CompilationOutcomeDescriptor compileWithSpecificCompiler(
        CompilationRequest compilationRequest,
        String sourceOutputDir,
        String classOutputDir,
        String additionalCompilerClasspath);

    protected void setupDirectories(Method testMethod, Class<?> testClass) throws Exception {
        String compilationRoot = getBasePath()
            + TARGET_COMPILATION_TESTS
            + testClass.getName()
            + "/" + testMethod.getName()
            + getPathSuffix();

        classOutputDir = compilationRoot + "/classes";
        sourceOutputDir = compilationRoot + "/generated-sources";
        additionalCompilerClasspath = compilationRoot + "/compiler";

        createOutputDirs();

        ( (ModifiableURLClassLoader) Thread.currentThread().getContextClassLoader() ).withPath( classOutputDir );
    }

    private String getPathSuffix() {
        return "_" + compiler.name().toLowerCase();
    }

    private void createOutputDirs() {
        File directory = new File( classOutputDir );
        deleteDirectory( directory );
        directory.mkdirs();

        directory = new File( sourceOutputDir );
        deleteDirectory( directory );
        directory.mkdirs();

        directory = new File( additionalCompilerClasspath );
        deleteDirectory( directory );
        directory.mkdirs();
    }

    static boolean needsRecompilation(CompilationRequest compilationRequest,
        CompilationCache compilationCache) {
        return !compilationRequest.equals( compilationCache.getLastRequest() );
    }


    /**
     * Returns the classes to be compiled for this test.
     *
     * @param testMethod
     * @param testClass
     *
     * @return A set containing the classes to be compiled for this test
     */
    private static Set<Class<?>> getTestClasses(Method testMethod, Class<?> testClass) {
        final Set<Class<?>> testClasses = new HashSet<Class<?>>();


        Optional<WithClasses> withClasses = AnnotationSupport.findAnnotation( testMethod, WithClasses.class );
        Consumer<WithClasses> consumer = new Consumer<WithClasses>() {
            @Override
            public void accept(WithClasses classes) {
                testClasses.addAll( Arrays.asList( classes.value() ) );
            }
        };
        withClasses.ifPresent( consumer );

        withClasses = AnnotationSupport.findAnnotation( testClass, WithClasses.class );
        withClasses.ifPresent( consumer );

        if ( testClasses.isEmpty() ) {
            throw new IllegalStateException(
                "The classes to be compiled during the test must be specified via @WithClasses."
            );
        }

        return testClasses;
    }
}
