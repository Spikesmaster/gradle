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

package org.gradle.api.plugins.migration.internal

import org.gradle.tooling.model.internal.migration.Archive

import com.google.common.collect.Sets

import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

class ArchivesComparator {
    private final ProjectComparison projectComparison
    private final BuildComparisonListener listener

    ArchivesComparator(ProjectComparison projectComparison, BuildComparisonListener listener) {
        this.projectComparison = projectComparison
        this.listener = listener
    }

    void compareArchives(Set<Archive> archives1, Set<Archive> archives2) {
        def archivesByName1 = archives1.collectEntries { [it.file.name, it] }
        def archivesByName2 = archives2.collectEntries { [it.file.name, it] }

        handleCommonArchives(archivesByName1, archivesByName2)
        handleOrphanArchives(projectComparison.project1, archivesByName1, archivesByName2)
        handleOrphanArchives(projectComparison.project2, archivesByName2, archivesByName1)
    }

    private void handleCommonArchives(Map<String, Archive> archivesByName1, Map<String, Archive> archivesByName2) {
        def commonArchiveNames = Sets.intersection(archivesByName1.keySet(), archivesByName2.keySet())
        for (name in commonArchiveNames) {
            def archive1 = archivesByName1[name]
            def archive2 = archivesByName2[name]
            def archiveComparison = new ArchiveComparison(parent: projectComparison)
            archiveComparison.archive1 = new ComparedArchive(parent: projectComparison.project1, archiveFile: archive1.file)
            archiveComparison.archive2 = new ComparedArchive(parent: projectComparison.project2, archiveFile: archive2.file)
            projectComparison.archiveComparisons << archiveComparison

            listener.archiveComparisonStarted(archiveComparison)

            def archiveEntriesByPath1 = getArchiveEntriesByPath(archive1)
            def archiveEntriesByPath2 = getArchiveEntriesByPath(archive2)
            archiveComparison.archive1.numberOfEntries = archiveEntriesByPath1.size()
            archiveComparison.archive2.numberOfEntries = archiveEntriesByPath2.size()

            handleCommonArchiveEntries(archiveComparison, archiveEntriesByPath1, archiveEntriesByPath2)
            handleOrphanArchiveEntries(archiveComparison, archiveComparison.archive1, archiveEntriesByPath1, archiveEntriesByPath2)
            handleOrphanArchiveEntries(archiveComparison, archiveComparison.archive2, archiveEntriesByPath2, archiveEntriesByPath1)

            listener.archiveComparisonFinished(archiveComparison)
        }
    }

    private void handleOrphanArchives(ComparedProject project, Map<String, Archive> archivesByName, Map<String, Archive> otherArchivesByName) {
        def orphanArchiveNames = Sets.difference(archivesByName.keySet(), otherArchivesByName.keySet())
        for (name in orphanArchiveNames) {
            def archive = archivesByName[name]
            def comparedArchive = new ComparedArchive(parent: project, archiveFile: archive.file)
            projectComparison.orphanArchives << comparedArchive
            listener.orphanArchiveFound(comparedArchive)
        }
    }

    private void handleCommonArchiveEntries(ArchiveComparison archiveComparison, Map<String, ZipEntry> archiveEntriesByPath1, Map<String, ZipEntry> archiveEntriesByPath2) {
        def entryComparator = new ZipEntryComparator()
        def commonEntries = Sets.newTreeSet(Sets.intersection(archiveEntriesByPath1.keySet(), archiveEntriesByPath2.keySet()))
        for (entryName in commonEntries) {
            def entry1 = archiveEntriesByPath1[entryName]
            def entry2 = archiveEntriesByPath2[entryName]
            if (entryComparator.compare(entry1, entry2)) {
                def entryDifference = new ArchiveEntryComparison(parent: archiveComparison)
                entryDifference.entry1 = new ComparedArchiveEntry(parent: archiveComparison.archive1, path: entry1.name, directory: entry1.directory, size: entry1.size, crc: entry1.crc)
                entryDifference.entry2 = new ComparedArchiveEntry(parent: archiveComparison.archive2, path: entry2.name, directory: entry2.directory, size: entry2.size, crc: entry2.crc)
                archiveComparison.entryDifferences << entryDifference
                listener.archiveEntryDifferenceFound(entryDifference)
            }
        }
    }

    private void handleOrphanArchiveEntries(ArchiveComparison archiveComparison, ComparedArchive archive, Map<String, ZipEntry> archiveEntries, Map<String, ZipEntry> otherArchiveEntries) {
        def orphanEntryNames1 = Sets.newTreeSet(Sets.difference(archiveEntries.keySet(), otherArchiveEntries.keySet()))
        for (entryName in orphanEntryNames1) {
            def entry = archiveEntries[entryName]
            def comparedEntry = new ComparedArchiveEntry(parent: archive, path: entry.name, directory: entry.directory, size: entry.size, crc: entry.crc)
            archiveComparison.orphanEntries << comparedEntry
            listener.orphanArchiveEntryFound(comparedEntry)
        }
    }

    private Map<String, ZipEntry> getArchiveEntriesByPath(Archive archive) {
        def result = [:]

        archive.file.withInputStream { stream ->
            ZipInputStream zipStream = new ZipInputStream(stream)
            def entry = zipStream.nextEntry
            while (entry != null) {
                result.put(entry.name, entry)
                entry = zipStream.nextEntry
            }
        }

        result
    }
}