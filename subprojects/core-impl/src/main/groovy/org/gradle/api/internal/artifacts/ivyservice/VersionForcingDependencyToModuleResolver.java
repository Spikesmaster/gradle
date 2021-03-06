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
package org.gradle.api.internal.artifacts.ivyservice;

import org.apache.ivy.core.module.descriptor.DependencyDescriptor;
import org.apache.ivy.core.module.id.ModuleId;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.gradle.api.Action;
import org.gradle.api.artifacts.ForcedModuleDetails;
import org.gradle.api.artifacts.ModuleVersionSelector;
import org.gradle.api.internal.artifacts.DefaultModuleVersionSelector;
import org.gradle.api.internal.artifacts.ivyservice.forcing.DefaultForcedModuleDetails;

public class VersionForcingDependencyToModuleResolver implements DependencyToModuleVersionIdResolver {
    private final DependencyToModuleVersionIdResolver resolver;
    private final Iterable<Action<ForcedModuleDetails>> rules;

    public VersionForcingDependencyToModuleResolver(DependencyToModuleVersionIdResolver resolver, Iterable<Action<ForcedModuleDetails>> rules) {
        this.resolver = resolver;
        this.rules = rules;
    }

    public ModuleVersionIdResolveResult resolve(DependencyDescriptor dependencyDescriptor) {
        for (Action<ForcedModuleDetails> rule : rules) {
            ModuleVersionSelector module = new DefaultModuleVersionSelector(dependencyDescriptor.getDependencyRevisionId().getOrganisation(), dependencyDescriptor.getDependencyRevisionId().getName(), dependencyDescriptor.getDependencyRevisionId().getRevision());
            DefaultForcedModuleDetails details = new DefaultForcedModuleDetails(module);
            rule.execute(details);
            if (details.getForcedVersion() != null) {
                ModuleId moduleId = new ModuleId(details.getModule().getGroup(), details.getModule().getName());
                ModuleRevisionId revisionId = new ModuleRevisionId(moduleId, details.getForcedVersion());
                DependencyDescriptor descriptor = dependencyDescriptor.clone(revisionId);
                ModuleVersionIdResolveResult result = resolver.resolve(descriptor);
                return new ForcedModuleVersionIdResolveResult(result);
            }
        }
        return resolver.resolve(dependencyDescriptor);
    }
}