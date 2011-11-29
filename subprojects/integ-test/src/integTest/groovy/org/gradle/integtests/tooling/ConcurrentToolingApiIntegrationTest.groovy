/*
 * Copyright 2011 the original author or authors.
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

package org.gradle.integtests.tooling

import org.gradle.integtests.fixtures.GradleDistribution
import org.gradle.integtests.tooling.fixture.ToolingApi
import org.gradle.tooling.ProjectConnection
import org.gradle.tooling.model.idea.IdeaProject
import org.gradle.util.ConcurrentSpecification
import spock.lang.Issue

// TODO - this needs to cover cross-version compatibility (ie can run concurrent builds for any target gradle >= 1.0-milestone-7)
// TODO - should cover concurrent builds each with a different target gradle version
class ConcurrentToolingApiIntegrationTest extends ConcurrentSpecification {

    def dist = new GradleDistribution()
    def toolingApi = new ToolingApi(dist)

    @Issue("GRADLE-1933")
    def "handles concurrent scenario"() {
        dist.file('build.gradle')  << """
apply plugin: 'java'
        """

        when:
        shortTimeout = 30000

        10.times {
            start { fetchModel() }
        }

        then:
        //it deals with concurrency issues, may not fail every single time
        finished()
    }

    def fetchModel() {
        toolingApi.withConnection { ProjectConnection connection ->
            try {
                def model = connection.getModel(IdeaProject)
                assert model != null
                //a bit more stress:
                connection.newBuild().forTasks('tasks').run()
            } catch (Exception e) {
                throw new RuntimeException("""Looks like we've hit a concurrency problem.
See the full stacktrace and the list of causes to investigate""", e);
            }
        }
    }
}
