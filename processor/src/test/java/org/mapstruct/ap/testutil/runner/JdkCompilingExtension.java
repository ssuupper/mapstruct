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
import static org.mapstruct.ap.testutil.runner.CompilingStatement.getSourceFiles;
import static org.mapstruct.ap.testutil.runner.CompilingStatement.loadAndInstantiate;
import static org.mapstruct.ap.testutil.runner.JdkCompilingStatement.COMPILER_CLASSPATH_FILES;
import static org.mapstruct.ap.testutil.runner.JdkCompilingStatement.DEFAULT_PROCESSOR_CLASSLOADER;

/**
 * @author Filip Hrisafov
 */
public class JdkCompilingExtension extends CompilingExtension {


    public JdkCompilingExtension() {
        super( Compiler.JDK );
    }


    @Override
    protected CompilationOutcomeDescriptor compileWithSpecificCompiler(CompilationRequest compilationRequest,
        String sourceOutputDir, String classOutputDir, String additionalCompilerClasspath) {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<JavaFileObject>();
        StandardJavaFileManager fileManager = compiler.getStandardFileManager( null, null, null );

        Iterable<? extends JavaFileObject> compilationUnits =
            fileManager.getJavaFileObjectsFromFiles( getSourceFiles( compilationRequest.getSourceClasses() ) );

        try {
            fileManager.setLocation( StandardLocation.CLASS_PATH, COMPILER_CLASSPATH_FILES );
            fileManager.setLocation( StandardLocation.CLASS_OUTPUT, Arrays.asList( new File( classOutputDir ) ) );
            fileManager.setLocation( StandardLocation.SOURCE_OUTPUT, Arrays.asList( new File( sourceOutputDir ) ) );
        }
        catch ( IOException e ) {
            throw new RuntimeException( e );
        }

        ClassLoader processorClassloader;
        if ( additionalCompilerClasspath == null ) {
            processorClassloader = DEFAULT_PROCESSOR_CLASSLOADER;
        }
        else {
            processorClassloader = new ModifiableURLClassLoader(
                new FilteringParentClassLoader( "org.mapstruct." ) )
                .withPaths( PROCESSOR_CLASSPATH )
                .withPath( additionalCompilerClasspath )
                .withOriginsOf( compilationRequest.getServices().values() );
        }

        JavaCompiler.CompilationTask task =
            compiler.getTask(
                null,
                fileManager,
                diagnostics,
                compilationRequest.getProcessorOptions(),
                null,
                compilationUnits );

        task.setProcessors(
            Arrays.asList( (Processor) loadAndInstantiate( processorClassloader, MappingProcessor.class ) ) );

        boolean compilationSuccessful = task.call();

        return CompilationOutcomeDescriptor.forResult(
            SOURCE_DIR,
            compilationSuccessful,
            diagnostics.getDiagnostics() );
    }
}
