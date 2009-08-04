/*******************************************************************************
 * Copyright (c) 2007, 2009 Codehaus.org, SpringSource, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Andy Clement        - Initial API and implementation
 *     Andrew Eisenberg - additional work
 *******************************************************************************/
package org.codehaus.jdt.groovy.integration;

import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.WorkingCopyOwner;
import org.eclipse.jdt.internal.compiler.CompilationResult;
import org.eclipse.jdt.internal.compiler.ast.CompilationUnitDeclaration;
import org.eclipse.jdt.internal.compiler.env.ICompilationUnit;
import org.eclipse.jdt.internal.compiler.lookup.LookupEnvironment;
import org.eclipse.jdt.internal.compiler.parser.Parser;
import org.eclipse.jdt.internal.compiler.problem.ProblemReporter;
import org.eclipse.jdt.internal.core.CompilationUnit;
import org.eclipse.jdt.internal.core.PackageFragment;
import org.eclipse.jdt.internal.core.util.CommentRecorderParser;
import org.eclipse.jdt.internal.core.util.Util;

/**
 * The default implementation just does what JDT would do.
 */
class DefaultLanguageSupport implements LanguageSupport {

	public Parser getParser(LookupEnvironment lookupEnvironment, ProblemReporter problemReporter, boolean parseLiteralExpressionsAsConstants,int variant) {
		if (variant==1) {
			return new Parser(problemReporter, parseLiteralExpressionsAsConstants);
		} else { // if (variant==2) {
			return new CommentRecorderParser(problemReporter, parseLiteralExpressionsAsConstants);
		}
	}
	
    public CompilationUnit newCompilationUnit(PackageFragment parent,
            String name, WorkingCopyOwner owner) {
        return new CompilationUnit(parent, name, owner);
    }

	public CompilationUnitDeclaration newCompilationUnitDeclaration(
			ICompilationUnit unit,
			ProblemReporter problemReporter, CompilationResult compilationResult, int sourceLength) {
		return new CompilationUnitDeclaration(problemReporter, compilationResult, sourceLength);
	}

    public boolean isInterestingProject(IProject project) {
        // assume that if this method is called, them this is a Java project
        return true;
    }

    public boolean isSourceFile(String fileName, boolean isInterestingProject) {
        return Util.isJavaLikeFileName(fileName);
    }

	
}