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

package org.gradle.api.publish.ivy.tasks;

import org.gradle.api.Buildable;
import org.gradle.api.DefaultTask;
import org.gradle.api.Incubating;
import org.gradle.api.InvalidUserDataException;
import org.gradle.api.artifacts.repositories.IvyArtifactRepository;
import org.gradle.api.file.FileCollection;
import org.gradle.api.internal.artifacts.ArtifactPublicationServices;
import org.gradle.api.internal.artifacts.ArtifactPublisher;
import org.gradle.api.publish.internal.PublishOperation;
import org.gradle.api.publish.ivy.IvyPublication;
import org.gradle.api.publish.ivy.internal.IvyNormalizedPublication;
import org.gradle.api.publish.ivy.internal.IvyPublicationInternal;
import org.gradle.api.publish.ivy.internal.IvyPublisher;
import org.gradle.api.tasks.TaskAction;

import javax.inject.Inject;
import java.util.concurrent.Callable;

/**
 * Publishes an IvyPublication to an IvyArtifactRepository.
 *
 * @since 1.3
 */
@Incubating
public class PublishToIvyRepository extends DefaultTask {

    private IvyPublicationInternal publication;
    private IvyArtifactRepository repository;

    private final ArtifactPublicationServices publicationServices;

    @Inject
    public PublishToIvyRepository(ArtifactPublicationServices publicationServices) {
        this.publicationServices = publicationServices;

        // Allow the publication to participate in incremental build
        getInputs().files(new Callable<FileCollection>() {
            public FileCollection call() throws Exception {
                IvyPublicationInternal publicationInternal = getPublicationInternal();
                return publicationInternal == null ? null : publicationInternal.getPublishableFiles();
            }
        });

        // Allow the publication to have its dependencies fulfilled
        // There may be dependencies that aren't about creating files and not covered above
        dependsOn(new Callable<Buildable>() {
            public Buildable call() throws Exception {
                IvyPublicationInternal publicationInternal = getPublicationInternal();
                return publicationInternal == null ? null : publicationInternal;
            }
        });

        // Should repositories be able to participate in incremental?
        // At the least, they may be able to express themselves as output files
        // They *might* have input files and other dependencies as well though
        // Inputs: The credentials they need may be expressed in a file
        // Dependencies: Can't think of a case here
    }

    /**
     * The publication to be published.
     *
     * @return The publication to be published
     */
    public IvyPublication getPublication() {
        return publication;
    }

    /**
     * Sets the publication to be published.
     *
     * @param publication The publication to be published
     */
    public void setPublication(IvyPublication publication) {
        this.publication = toPublicationInternal(publication);
    }

    private IvyPublicationInternal getPublicationInternal() {
        return toPublicationInternal(getPublication());
    }

    private static IvyPublicationInternal toPublicationInternal(IvyPublication publication) {
        if (publication == null) {
            return null;
        } else if (publication instanceof IvyPublicationInternal) {
            return (IvyPublicationInternal) publication;
        } else {
            throw new InvalidUserDataException(
                    String.format(
                            "publication objects must implement the '%s' interface, implementation '%s' does not",
                            IvyPublicationInternal.class.getName(),
                            publication.getClass().getName()
                    )
            );
        }
    }

    /**
     * The repository to publish to.
     *
     * @return The repository to publish to
     */
    public IvyArtifactRepository getRepository() {
        return repository;
    }

    /**
     * Sets the repository to publish to.
     *
     * @param repository The repository to publish to
     */
    public void setRepository(IvyArtifactRepository repository) {
        this.repository = repository;
    }

    @TaskAction
    public void publish() {
        IvyPublicationInternal publicationInternal = getPublicationInternal();
        if (publicationInternal == null) {
            throw new InvalidUserDataException("The 'publication' property is required");
        }

        IvyArtifactRepository repository = getRepository();
        if (repository == null) {
            throw new InvalidUserDataException("The 'repository' property is required");
        }

        doPublish(publicationInternal, repository);
    }

    private void doPublish(final IvyPublicationInternal publication, final IvyArtifactRepository repository) {
        new PublishOperation(publication, repository) {
            @Override
            protected void publish() throws Exception {
                ArtifactPublisher artifactPublisher = publicationServices.createArtifactPublisher();
                IvyNormalizedPublication normalizedPublication = publication.asNormalisedPublication();
                IvyPublisher publisher = new IvyPublisher(artifactPublisher);
                publisher.publish(normalizedPublication, repository);
            }
        }.run();
    }

}
