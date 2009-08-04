/*******************************************************************************
 * Copyright (c) 2009 Codehaus.org, SpringSource, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Andy Clement        - Initial API and implementation
 *     Andrew Eisenberg - Additional work
 *******************************************************************************/
package org.codehaus.jdt.groovy.integration.internal;

import java.util.Collections;

import org.codehaus.jdt.groovy.internal.compiler.ast.GroovyParser;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.groovy.core.util.ContentTypeUtils;
import org.eclipse.jdt.internal.compiler.CompilationResult;
import org.eclipse.jdt.internal.compiler.IProblemFactory;
import org.eclipse.jdt.internal.compiler.SourceElementNotifier;
import org.eclipse.jdt.internal.compiler.SourceElementParser;
import org.eclipse.jdt.internal.compiler.ast.AbstractMethodDeclaration;
import org.eclipse.jdt.internal.compiler.ast.CompilationUnitDeclaration;
import org.eclipse.jdt.internal.compiler.ast.FieldDeclaration;
import org.eclipse.jdt.internal.compiler.ast.TypeDeclaration;
import org.eclipse.jdt.internal.compiler.env.ICompilationUnit;
import org.eclipse.jdt.internal.compiler.impl.CompilerOptions;
import org.eclipse.jdt.internal.compiler.problem.ProblemReporter;
import org.eclipse.jdt.internal.compiler.util.HashtableOfObjectToInt;
import org.eclipse.jdt.internal.compiler.ISourceElementRequestor;

/**
 * The multiplexing parser can delegate file parsing to multiple parsers. In this scenario it subtypes 'Parser' (which is the Java
 * parser) but is also aware of a groovy parser. Depending on what kind of file is to be parsed, it will invoke the relevant parser.
 * 
 * @author Andrew Eisenberg
 */
public class MultiplexingSourceElementRequestorParser extends SourceElementParser {

	ISourceElementRequestor groovyRequestor;

	SourceElementNotifier notifier;

	boolean groovyReportReferenceInfo;

	ProblemReporter problemReporter;

	public MultiplexingSourceElementRequestorParser(ProblemReporter problemReporter, ISourceElementRequestor requestor,
			IProblemFactory problemFactory, CompilerOptions options, boolean reportLocalDeclarations,
			boolean optimizeStringLiterals, boolean useSourceJavadocParser) {
		super(requestor, problemFactory, options, reportLocalDeclarations, optimizeStringLiterals, useSourceJavadocParser);
		// The superclass that is extended is in charge of parsing .java files
		this.groovyRequestor = requestor;
		this.notifier = new SourceElementNotifier(requestor, reportLocalDeclarations);
		this.groovyReportReferenceInfo = reportLocalDeclarations;
		this.problemReporter = problemReporter;
	}

	public MultiplexingSourceElementRequestorParser(ProblemReporter problemReporter, ISourceElementRequestor requestor,
			IProblemFactory problemFactory, CompilerOptions options, boolean reportLocalDeclarations, boolean optimizeStringLiterals) {
		super(requestor, problemFactory, options, reportLocalDeclarations, optimizeStringLiterals);
		// The superclass that is extended is in charge of parsing .java files
		this.groovyRequestor = requestor;
		this.notifier = new SourceElementNotifier(requestor, reportLocalDeclarations);
		this.problemReporter = problemReporter;
	}

	@Override
	public CompilationUnitDeclaration parseCompilationUnit(ICompilationUnit unit, boolean fullParse, IProgressMonitor pm) {

		if (ContentTypeUtils.isGroovyLikeFileName(unit.getFileName())) {
			// ASSUMPTIONS:
			// 1) there is no difference between a diet and full parse in the groovy works, so can ignore the fullParse parameter
			// 2) parsing is for the entire CU (ie- from character 0, to unit.getContents().length)
			// 3) nodesToCategories map is not necessary. I think it has something to do with JavaDoc, but not sure

			CompilationResult compilationResult = new CompilationResult(unit, 0, 0, this.options.maxProblemsPerUnit);

			// FIXASC (M2) Is it ok to use a new parser here everytime? If we don't we sometimes recurse back into the first one
			CompilationUnitDeclaration cud = new GroovyParser(null, problemReporter).dietParse(unit, compilationResult);

			// CompilationUnitDeclaration cud groovyParser.dietParse(sourceUnit, compilationResult);
			HashtableOfObjectToInt sourceEnds = createSourceEnds(cud);

			// FIXADE (M2) what should go in the nodesToCategories map (ie- the last argument)?
			notifier.notifySourceElementRequestor(cud, 0, unit.getContents().length, groovyReportReferenceInfo, sourceEnds,
					Collections.EMPTY_MAP);
			return cud;
		} else {
			return super.parseCompilationUnit(unit, fullParse, pm);
		}
	}

	// TODO This should be calculated in GroovyCompilationUnitDeclaration
	private HashtableOfObjectToInt createSourceEnds(CompilationUnitDeclaration cDecl) {
		HashtableOfObjectToInt table = new HashtableOfObjectToInt();
		if (cDecl.types != null) {
			for (TypeDeclaration tDecl : cDecl.types) {
				createSourceEndsForType(tDecl, table);
			}
		}
		return table;
	}

	// TODO This should be calculated in GroovyCompilationUnitDeclaration
	private void createSourceEndsForType(TypeDeclaration tDecl, HashtableOfObjectToInt table) {
		table.put(tDecl, tDecl.sourceEnd);
		if (tDecl.fields != null) {
			for (FieldDeclaration fDecl : tDecl.fields) {
				table.put(fDecl, fDecl.sourceEnd);
			}
		}
		if (tDecl.methods != null) {
			for (AbstractMethodDeclaration mDecl : tDecl.methods) {
				table.put(mDecl, mDecl.sourceEnd);
			}
		}
		if (tDecl.memberTypes != null) {
			for (TypeDeclaration innerTDecl : tDecl.memberTypes) {
				createSourceEndsForType(innerTDecl, table);
			}
		}
	}

}