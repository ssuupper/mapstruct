package org.mapstruct.ap.testutil.runner;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestInstanceFactory;
import org.junit.jupiter.api.extension.TestInstanceFactoryContext;
import org.junit.jupiter.api.extension.TestInstantiationException;
import org.junit.platform.commons.util.ReflectionUtils;

/**
 * @author Filip Hrisafov
 */
public class CustomClassLoaderFactory implements TestInstanceFactory {
    @Override
    public Object createTestInstance(TestInstanceFactoryContext factoryContext, ExtensionContext extensionContext)
        throws TestInstantiationException {
        return ReflectionUtils.newInstance( InnerAnnotationProcessorRunner.replaceClassLoaderAndClass( factoryContext.getTestClass() ) );
    }
}
