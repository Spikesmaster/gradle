/*
 * Copyright 2012 the original author or authors.
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

package org.gradle.api.publish.ivy.tasks.internal

import org.gradle.api.internal.tasks.DefaultTaskDependency
import org.gradle.api.publish.Publication
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.ivy.IvyModuleDescriptor
import org.gradle.api.publish.ivy.IvyPublication
import org.gradle.api.publish.ivy.internal.IvyModuleDescriptorInternal
import org.gradle.api.publish.ivy.internal.IvyPublicationInternal
import org.gradle.api.publish.ivy.tasks.GenerateIvyDescriptor
import org.gradle.api.publish.plugins.PublishingPlugin
import org.gradle.util.HelperUtil
import spock.lang.Specification

class IvyPublicationDynamicDescriptorGenerationTaskCreatorTest extends Specification {

    def project = HelperUtil.createRootProject()
    def creator = new IvyPublicationDynamicDescriptorGenerationTaskCreator(project.tasks)
    PublishingExtension publishing

    def setup() {
        project.plugins.apply(PublishingPlugin)
        publishing = project.extensions.getByType(PublishingExtension)
        creator.monitor(publishing.publications)
    }

    def "creates tasks"() {
        expect:
        descriptorGeneratorTasks.size() == 0

        when:
        publishing.repositories.ivy { }
        publishing.publications.add(publication("foo"))

        then:
        descriptorGeneratorTasks.size() == 0

        when:
        publishing.publications.add(ivyPublication("ivy"))

        then:
        descriptorGeneratorTasks.size() == 1
        GenerateIvyDescriptor task = project.tasks.generateIvyModuleDescriptor
        task.group == "publishing"
        task.description != null
        publishing.publications.ivy.descriptor.buildDependencies.getDependencies(null) == [task] as Set

        when:
        publishing.publications.add(ivyPublication("other"))

        then:
        descriptorGeneratorTasks.size() == 2
        def task2 = project.tasks.generateOtherIvyModuleDescriptor
        publishing.publications.other.descriptor.buildDependencies.getDependencies(null) == [task2] as Set
    }

    Publication publication(String name) {
        Mock(Publication) {
            getName() >> name
        }
    }

    IvyPublication ivyPublication(String name) {
        def taskDependency = new DefaultTaskDependency(project.tasks)
        IvyModuleDescriptor moduleDescriptor = Mock(IvyModuleDescriptorInternal) {
            builtBy(*_) >> { taskDependency.add(it) }
            getBuildDependencies() >> taskDependency
        }
        Mock(IvyPublicationInternal) {
            getName() >> name
            getDescriptor() >> moduleDescriptor
        }
    }

    def getDescriptorGeneratorTasks() {
        project.tasks.withType(GenerateIvyDescriptor)
    }

}
