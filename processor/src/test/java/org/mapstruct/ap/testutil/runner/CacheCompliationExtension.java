package org.mapstruct.ap.testutil.runner;

import java.util.function.Function;

import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import static org.mapstruct.ap.testutil.runner.CompilingExtension.ANNOTATION;

/**
 * @author Filip Hrisafov
 */
public class CacheCompliationExtension implements BeforeAllCallback {

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        ExtensionContext.Store annotationStore = context.getStore( ANNOTATION );
        for ( Compiler compiler : Compiler.values() ) {
            CompilationCache compilationCache = annotationStore
                .getOrComputeIfAbsent( compiler.name() + "-compilationCache", new Function<String, CompilationCache>() {

                    @Override
                    public CompilationCache apply(String s) {
                        return new CompilationCache();
                    }
                }, CompilationCache.class );
        }
    }
}
