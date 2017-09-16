package org.mapstruct.ap.testutil.runner;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import javax.annotation.processing.Processor;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;

import org.mapstruct.ap.MappingProcessor;
import org.mapstruct.ap.testutil.compilation.model.CompilationOutcomeDescriptor;

import static org.mapstruct.ap.testutil.runner.CompilingStatement.PROCESSOR_CLASSPATH;
import static org.mapstruct.ap.testutil.runner.CompilingStatement.SOURCE_DIR;
import static org.mapstruct.ap.testutil.runner.CompilingStatement.TEST_COMPILATION_CLASSPATH;
import static org.mapstruct.ap.testutil.runner.CompilingStatement.getSourceFiles;
import static org.mapstruct.ap.testutil.runner.CompilingStatement.loadAndInstantiate;
import static org.mapstruct.ap.testutil.runner.EclipseCompilingStatement.DEFAULT_ECLIPSE_COMPILER_CLASSLOADER;
import static org.mapstruct.ap.testutil.runner.EclipseCompilingStatement.ECLIPSE_COMPILER_CLASSPATH;
import static org.mapstruct.ap.testutil.runner.EclipseCompilingStatement.newFilteringClassLoaderForEclipse;
import static org.mapstruct.ap.testutil.runner.JdkCompilingStatement.COMPILER_CLASSPATH_FILES;
import static org.mapstruct.ap.testutil.runner.JdkCompilingStatement.DEFAULT_PROCESSOR_CLASSLOADER;

/**
 * @author Filip Hrisafov
 */
public class EclipseCompilingExtension extends CompilingExtension {


    public EclipseCompilingExtension() {
        super( Compiler.ECLIPSE );
    }


    @Override
    protected CompilationOutcomeDescriptor compileWithSpecificCompiler(CompilationRequest compilationRequest,
        String sourceOutputDir, String classOutputDir, String additionalCompilerClasspath) {
        ClassLoader compilerClassloader;
        if ( additionalCompilerClasspath == null ) {
            compilerClassloader = DEFAULT_ECLIPSE_COMPILER_CLASSLOADER;
        }
        else {
            ModifiableURLClassLoader loader = new ModifiableURLClassLoader(
                newFilteringClassLoaderForEclipse()
                    .hidingClasses( compilationRequest.getServices().values() ) );

            compilerClassloader = loader.withPaths( ECLIPSE_COMPILER_CLASSPATH )
                .withPaths( PROCESSOR_CLASSPATH )
                .withOriginOf( EclipseCompilingStatement.ClassLoaderExecutor.class )
                .withPath( additionalCompilerClasspath )
                .withOriginsOf( compilationRequest.getServices().values() );
        }

        EclipseCompilingStatement.ClassLoaderHelper clHelper =
            (EclipseCompilingStatement.ClassLoaderHelper) loadAndInstantiate( compilerClassloader, EclipseCompilingStatement.ClassLoaderExecutor.class );

        return clHelper.compileInOtherClassloader(
            compilationRequest,
            TEST_COMPILATION_CLASSPATH,
            getSourceFiles( compilationRequest.getSourceClasses() ),
            SOURCE_DIR,
            sourceOutputDir,
            classOutputDir );
    }
}
