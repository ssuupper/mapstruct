package org.mapstruct.ap.testutil.runner;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.TestTemplateInvocationContext;

/**
 * @author Filip Hrisafov
 */
public class JdkTemplateInvocationContext implements TestTemplateInvocationContext {

    @Override
    public String getDisplayName(int invocationIndex) {
        return "[jdk]";
    }

    @Override
    public List<Extension> getAdditionalExtensions() {
        List<Extension> extensions = new ArrayList<Extension>();
        extensions.add( new JdkCompilingExtension() );
        return extensions;
    }
}
