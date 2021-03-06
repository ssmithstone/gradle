/*
 * Copyright 2010 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gradle.plugins.ide.idea

import org.gradle.plugins.ide.api.XmlGeneratorTask
import org.gradle.plugins.ide.idea.model.IdeaModule
import org.gradle.plugins.ide.idea.model.Module

/**
 * Generates an IDEA module file. If you want to fine tune the idea configuration
 * <p>
 * Please refer to interesting examples on idea configuration in {@link IdeaModule}.
 * <p>
 * At this moment nearly all configuration is done via {@link IdeaModule}.
 *
 * @author Hans Dockter
 */
public class GenerateIdeaModule extends XmlGeneratorTask<Module> {

    /**
     * Idea module model
     */
    IdeaModule module

    @Override protected Module create() {
        new Module(xmlTransformer, module.pathFactory)
    }

    @Override protected void configure(Module xmlModule) {
        getModule().mergeXmlModule(xmlModule)
    }

    /**
     * Configures output *.iml file. It's <b>optional</b> because the task should configure it correctly for you
     * (including making sure it is unique in the multi-module build).
     * If you really need to change the output file name it is much easier to do it via the <b>idea.module.name</b> property.
     * <p>
     * Please refer to documentation in {@link IdeaModule} <b>name</b> property. In IntelliJ IDEA the module name is the same as the name of the *.iml file.
     */
    File getOutputFile() {
        return module.outputFile
    }

    void setOutputFile(File newOutputFile) {
        module.outputFile = newOutputFile
    }

}
