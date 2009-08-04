/*******************************************************************************
 * Copyright (c) 2007, 2009 Codehaus.org, SpringSource, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Unattributed        - Initial API and implementation
 *     Andrew Eisenberg - modified for Groovy Eclipse 2.0
 *******************************************************************************/
package org.codehaus.groovy.eclipse.core.model;

import static org.codehaus.groovy.eclipse.core.util.ListUtil.newEmptyList;

import java.util.List;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.codehaus.groovy.eclipse.core.GroovyCore;
import org.codehaus.groovy.eclipse.core.builder.GroovyClasspathContainer;
import org.codehaus.jdt.groovy.model.GroovyNature;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.junit.buildpath.BuildPathSupport;

/**
 * This class contains all the utility methods used in adding the Groovy Runtime
 * to a Java project.
 */
public class GroovyRuntime {

    private static final String junitJarName = "junit.jar";

    public static void removeGroovyNature(final IProject project)
            throws CoreException {
        GroovyCore.trace("GroovyRuntime.removeGroovyNature()");
        final IProjectDescription description = project.getDescription();
        final String[] ids = description.getNatureIds();
        for (int i = 0; i < ids.length; ++i) {
            if (ids[i].equals(GroovyNature.GROOVY_NATURE)) {
                final String[] newIds = (String[]) ArrayUtils.remove(ids, i);
                description.setNatureIds(newIds);
                project.setDescription(description, null);
                return;
            }
        }
    }

    public static void addJunitSupport(final IJavaProject project)
            throws JavaModelException {
        GroovyCore.trace("GroovyRuntime.addJunitSupprt()");
        boolean junitExists = includesClasspathEntry(project, junitJarName);
        if (junitExists) {
            return;
        }
        addClassPathEntry(project, BuildPathSupport.getJUnit3ClasspathEntry());
    }

    public static void removeLibraryFromClasspath(
            final IJavaProject javaProject, final IPath libraryPath)
            throws JavaModelException {
        final IClasspathEntry[] oldEntries = javaProject.getRawClasspath();
        for (int i = 0; i < oldEntries.length; i++) {
            final IClasspathEntry entry = oldEntries[i];
            if (entry.getPath().equals(libraryPath)) {
                final IClasspathEntry[] newEntries = (IClasspathEntry[]) ArrayUtils
                        .remove(oldEntries, i);
                javaProject.setRawClasspath(newEntries, null);
                return;
            }
        }
    }

    public static void addGroovyRuntime(final IProject project) {
        GroovyCore.trace("GroovyRuntime.addGroovyRuntime()");
        try {
            if (project == null || !project.hasNature(JavaCore.NATURE_ID))
                return;
            if (project.hasNature(GroovyNature.GROOVY_NATURE))
                return;

            addGroovyNature(project);
            final IJavaProject javaProject = JavaCore.create(project);
            addGroovyClasspathContainer(javaProject);

        } catch (final Exception e) {
            GroovyCore.logException("Failed to add groovy runtime support", e);
        }
    }

    public static boolean hasGroovyClasspathContainer(
            final IJavaProject javaProject) throws CoreException {
        if (javaProject == null || !javaProject.getProject().isAccessible())
            return false;
        final IClasspathEntry[] entries = javaProject.getRawClasspath();
        for (int i = 0; i < entries.length; i++) {
            final IClasspathEntry entry = entries[i];
            if (entry.getEntryKind() != IClasspathEntry.CPE_CONTAINER)
                continue;
            if (!ObjectUtils.equals(entry.getPath(),
                    GroovyClasspathContainer.CONTAINER_ID)
                    && !GroovyClasspathContainer.CONTAINER_ID.isPrefixOf(entry
                            .getPath()))
                continue;
            return true;
        }
        return false;
    }

    public static void excludeGroovyFilesFromOutput(
            final IJavaProject javaProject) {
        // make sure .groovy files are not copied to the output dir
        String excludedResources = javaProject.getOption(
                JavaCore.CORE_JAVA_BUILD_RESOURCE_COPY_FILTER, true);
        if (excludedResources.indexOf("*.groovy") == -1) {
            excludedResources = excludedResources.length() == 0 ? "*.groovy"
                    : excludedResources + ",*.groovy";
            javaProject.setOption(
                    JavaCore.CORE_JAVA_BUILD_RESOURCE_COPY_FILTER,
                    excludedResources);
        }
    }

    public static void includeGroovyFilesInOutput(final IJavaProject javaProject) {
        // make sure .groovy files are not copied to the output dir
        final String[] excludedResourcesArray = StringUtils.split(
                javaProject.getOption(
                        JavaCore.CORE_JAVA_BUILD_RESOURCE_COPY_FILTER, true),
                ",");
        final List<String> excludedResources = newEmptyList();
        for (int i = 0; i < excludedResourcesArray.length; i++) {
            final String excluded = excludedResourcesArray[i].trim();
            if (excluded.endsWith("*.groovy"))
                continue;
            excludedResources.add(excluded);
        }
        javaProject.setOption(JavaCore.CORE_JAVA_BUILD_RESOURCE_COPY_FILTER,
                StringUtils.join(excludedResources, ","));
    }

    public static void addGroovyClasspathContainer(
            final IJavaProject javaProject) {
        try {
            if (hasGroovyClasspathContainer(javaProject))
                return;

            final IClasspathEntry containerEntry = JavaCore.newContainerEntry(
                    GroovyClasspathContainer.CONTAINER_ID, true);
            addClassPathEntry(javaProject, containerEntry);
            excludeGroovyFilesFromOutput(javaProject);
        } catch (final CoreException ce) {
            GroovyCore.logException("Failed to add groovy classpath container:"
                    + ce.getMessage(), ce);
            throw new RuntimeException(ce);
        }
    }

    /**
     * Adds a library/folder that already exists in the project to the
     * classpath. Only added if it is not already on the classpath.
     * 
     * @param javaProject
     *            The project to add add the classpath entry to.
     * @param libraryPath
     *            The path to add to the classpath.
     * @throws JavaModelException
     */
    public static void addLibraryToClasspath(final IJavaProject javaProject,
            final IPath libraryPath) throws JavaModelException {
        boolean alreadyExists = includesClasspathEntry(javaProject, libraryPath
                .lastSegment());
        if (alreadyExists) {
            return;
        }
        addClassPathEntry(javaProject, JavaCore.newLibraryEntry(libraryPath,
                null, null, true));
    }

    private static void addGroovyNature(final IProject project)
            throws CoreException {
        GroovyCore.trace("GroovyRuntime.addGroovyNature()");
        final IProjectDescription description = project.getDescription();
        final String[] ids = description.getNatureIds();

        // add groovy nature at the start so that its image will be shown
        final String[] newIds = new String[ids == null ? 1 : ids.length + 1];
        newIds[0] = GroovyNature.GROOVY_NATURE;
        if (ids != null) {
            for (int i = 1; i < newIds.length; i++) {
                newIds[i] = ids[i - 1];
            }
        }

        description.setNatureIds(newIds);
        project.setDescription(description, null);
    }

    /**
     * Adds a classpath entry to a project
     * 
     * @param project
     *            The project to add the entry to.
     * @param newEntry
     *            The entry to add.
     * @throws JavaModelException
     */
    public static void addClassPathEntry(IJavaProject project,
            IClasspathEntry newEntry) throws JavaModelException {
        IClasspathEntry[] newEntries = (IClasspathEntry[]) ArrayUtils.add(
                project.getRawClasspath(), newEntry);
        project.setRawClasspath(newEntries, null);
    }

    /**
     * Looks through a set of classpath entries and checks to see if the path is
     * in them.
     * 
     * @param project
     *            The project to search.
     * @param possiblePath
     *            The path to check the entries for.
     * @return If possiblePath is included in entries returns true, otherwise
     *         returns false.
     * @throws JavaModelException
     */
    private static boolean includesClasspathEntry(IJavaProject project,
            String entryName) throws JavaModelException {
        IClasspathEntry[] entries = project.getResolvedClasspath(false);
        for (int i = 0; i < entries.length; i++) {
            IClasspathEntry entry = entries[i];
            if (entry.getPath().lastSegment().equals(entryName)) {
                return true;
            }
        }
        return false;
    }
}