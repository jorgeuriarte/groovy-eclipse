/*******************************************************************************
 * Copyright (c) 2011 Codehaus.org, SpringSource, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Andrew Eisenberg - Initial implemenation
 *******************************************************************************/
package org.codehaus.groovy.eclipse.dsl.pointcuts.impl;

import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.ModuleNode;
import org.codehaus.groovy.eclipse.GroovyLogManager;
import org.codehaus.groovy.eclipse.TraceCategory;
import org.codehaus.groovy.eclipse.dsl.pointcuts.AbstractPointcut;
import org.codehaus.groovy.eclipse.dsl.pointcuts.BindingSet;
import org.codehaus.groovy.eclipse.dsl.pointcuts.GroovyDSLDContext;
import org.codehaus.groovy.eclipse.dsl.pointcuts.PointcutVerificationException;
import org.codehaus.jdt.groovy.internal.compiler.ast.JDTClassNode;
import org.eclipse.jdt.core.compiler.CharOperation;

/**
 * Tests that the file of the current type of expression being analyzed is in 
 * the given source folder
 * 
 * Argument should be the workspace relative path to the source folder, using '/'
 * as a path separator.
 * @author andrew
 * @created Feb 10, 2011
 */
public class SourceFolderOfTypePointcut extends AbstractPointcut {

    public SourceFolderOfTypePointcut(String containerIdentifier) {
        super(containerIdentifier);
    }

    @Override
    public BindingSet matches(GroovyDSLDContext pattern) {
        if (extractFileName(pattern.getCurrentType(), pattern).startsWith((String) getFirstArgument())) {
            return new BindingSet().addDefaultBinding(pattern.fileName);
        } else {
            return null;
        }
    }
    

    private String extractFileName(ClassNode currentType, GroovyDSLDContext pattern) {
        ClassNode redirect = currentType.redirect();
        if (redirect instanceof JDTClassNode) {
            JDTClassNode jdtClass = (JDTClassNode) redirect;
            char[] fileName = jdtClass.getJdtBinding().getFileName();
            if (fileName != null) {
                // now remove the project name
                int slashIndex = CharOperation.indexOf('/', fileName);
                if (slashIndex >= 0) {
                    // need the second slash
                    slashIndex = CharOperation.indexOf('/', fileName, slashIndex+1);
                }
                if (slashIndex > 0) {
                    return String.valueOf(CharOperation.subarray(fileName, slashIndex+1, fileName.length));
                }
            }
        } else {
            // check if this type is in the current file 
            ModuleNode module = pattern.getCurrentScope().getEnclosingTypeDeclaration().getModule();
            if (module != null && module.getClasses().contains(redirect)) {
                return pattern.fileName;
            }
        }
        
        // will be "" for primitive and other core types loaded by ClassHelper.
        if (GroovyLogManager.manager.hasLoggers()) {
            GroovyLogManager.manager.log(TraceCategory.DSL, "Cannot find file for type " + currentType.getName());
        }
        return "";
    }

    @Override
    public void verify() throws PointcutVerificationException {
        String maybeStatus = allArgsAreStrings();
        if (maybeStatus != null) {
            throw new PointcutVerificationException(maybeStatus, this);
        }
        maybeStatus = hasOneArg();
        if (maybeStatus != null) {
            throw new PointcutVerificationException(maybeStatus, this);
        }
        super.verify();
    }
}
