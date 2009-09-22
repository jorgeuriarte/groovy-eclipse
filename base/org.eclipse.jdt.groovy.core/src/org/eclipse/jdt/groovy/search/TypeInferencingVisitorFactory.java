/*
 * Copyright 2003-2009 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.eclipse.jdt.groovy.search;

import org.codehaus.jdt.groovy.model.GroovyCompilationUnit;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.search.SearchPattern;
import org.eclipse.jdt.core.search.SearchRequestor;
import org.eclipse.jdt.groovy.search.ITypeLookup.LookupType;
import org.eclipse.jdt.internal.core.search.matching.PossibleMatch;
import org.eclipse.jdt.internal.core.search.matching.TypeDeclarationPattern;
import org.eclipse.jdt.internal.core.search.matching.TypeReferencePattern;
import org.eclipse.jdt.internal.core.util.Util;

/**
 * @author Andrew Eisenberg
 * @created Aug 31, 2009
 * 
 */
public class TypeInferencingVisitorFactory {

	/**
	 * Create a new {@link TypeInferencingVisitorWithRequestor}
	 * 
	 * @param typeRequestor
	 * @param possibleMatch
	 * @param pattern
	 * @param requestor
	 * @return a fully configured {@link TypeInferencingVisitorWithRequestor}
	 */
	public TypeInferencingVisitorWithRequestor createVisitor(ITypeRequestor typeRequestor, PossibleMatch possibleMatch,
			SearchPattern pattern, SearchRequestor requestor) {

		try {
			IFile file = ResourcesPlugin.getWorkspace().getRoot().getFile(new Path(possibleMatch.document.getPath()));
			GroovyCompilationUnit unit = (GroovyCompilationUnit) JavaCore.createCompilationUnitFrom(file);
			TypeInferencingVisitorWithRequestor visitor = new TypeInferencingVisitorWithRequestor(unit, createLookups(pattern));
			return visitor;
		} catch (Exception e) {
			Util.log(e, "Exception when creating TypeInferencingVisitorWithRequestor for " + possibleMatch.document.getPath()); //$NON-NLS-1$
		}
		return null;
	}

	// maybe this can be populated via an extension point.
	private ITypeLookup[] createLookups(SearchPattern pattern) {
		LookupType lookupType;
		if (pattern instanceof TypeDeclarationPattern || pattern instanceof TypeReferencePattern) {
			lookupType = LookupType.RETURN_TYPE;
		} else {
			lookupType = LookupType.DECLARING_TYPE;
		}
		ITypeLookup[] lookups = new ITypeLookup[] { new SimpleTypeLookup() };
		for (ITypeLookup lookup : lookups) {
			lookup.setLookupType(lookupType);
		}
		return lookups;

	}

}