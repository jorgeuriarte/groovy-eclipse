/*
 * Copyright 2011 the original author or authors.
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
package org.codehaus.groovy.eclipse.quickassist;

import java.util.ArrayList;
import java.util.List;

import org.codehaus.jdt.groovy.model.GroovyCompilationUnit;
import org.codehaus.jdt.groovy.model.GroovyNature;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.ui.text.java.IInvocationContext;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.jdt.ui.text.java.IProblemLocation;
import org.eclipse.jdt.ui.text.java.IQuickAssistProcessor;

public class GroovyQuickAssist implements IQuickAssistProcessor {

	public boolean hasAssists(IInvocationContext context) throws CoreException {
		if (context != null
				&& isContentInGroovyProject(context.getCompilationUnit())) {
			return new AddSuggestionsQuickAssistProposal(context).hasProposals() || 
			        new ConvertToClosureCompletionProposal(context).hasProposals() ||
			        new ConvertToMethodCompletionProposal(context).hasProposals() ||
                    new ConvertToMultiLineStringCompletionProposal(context).hasProposals() ||
                    new ConvertToSingleLineStringCompletionProposal(context).hasProposals() ||
                    new RemoveUnnecessarySemicolonsCompletionProposal(context).hasProposals() ||
                    new SwapOperandsCompletionProposal(context).hasProposals() ||
                    new SplitAssigmentCompletionProposal(context).hasProposals() ||
                    new AssignStatementToNewLocalProposal(context).hasProposals();
		}
		return false;
	}

	public IJavaCompletionProposal[] getAssists(IInvocationContext context,
			IProblemLocation[] locations) throws CoreException {
	    if (!(context.getCompilationUnit() instanceof GroovyCompilationUnit)) {
	        return new IJavaCompletionProposal[0];
	    }
		List<IJavaCompletionProposal> proposalList = new ArrayList<IJavaCompletionProposal>();
		
		AddSuggestionsQuickAssistProposal javaProposal = new AddSuggestionsQuickAssistProposal(
				context);
		if (javaProposal.hasProposals()) {
		    proposalList.add(javaProposal);
		}
		
		ConvertToClosureCompletionProposal convertToClosure = new ConvertToClosureCompletionProposal(context);
		if (convertToClosure.hasProposals()) {
            proposalList.add(convertToClosure);
        }
        
		ConvertToMethodCompletionProposal convertToMethod = new ConvertToMethodCompletionProposal(context);
		if (convertToMethod.hasProposals()) {
		    proposalList.add(convertToMethod);
		}
		
		ConvertToMultiLineStringCompletionProposal convertToMultiLineString = new ConvertToMultiLineStringCompletionProposal(context);
		if (convertToMultiLineString.hasProposals()) {
		    proposalList.add(convertToMultiLineString);
		}
		
		ConvertToSingleLineStringCompletionProposal convertToSingleLineString = new ConvertToSingleLineStringCompletionProposal(context);
		if (convertToSingleLineString.hasProposals()) {
		    proposalList.add(convertToSingleLineString);
		}
		
		RemoveUnnecessarySemicolonsCompletionProposal unnecessarySemicolons = new RemoveUnnecessarySemicolonsCompletionProposal(context);
		if (unnecessarySemicolons.hasProposals()) {
            proposalList.add(unnecessarySemicolons);
        }
		
		SplitAssigmentCompletionProposal splitAssignment = new SplitAssigmentCompletionProposal(context);
		if (splitAssignment.hasProposals()) {
		    proposalList.add(splitAssignment);
		}
		
		SwapOperandsCompletionProposal swapOperands = new SwapOperandsCompletionProposal(context);
		if (swapOperands.hasProposals()) {
		    proposalList.add(swapOperands);
		}
		
		AssignStatementToNewLocalProposal assignStatement = new AssignStatementToNewLocalProposal(context);
		if (assignStatement.hasProposals()) {
		    proposalList.add(assignStatement);
		}
		
		
		return proposalList.toArray(new IJavaCompletionProposal[0]);
	}
	
	/**
     * True if the problem is contained in an accessible (open and existing)
     * Groovy project in the workspace. False otherwise.
     * 
     * @param unit
     *            compilation unit containing the resource with the problem
     * @return true if and only if the problem is contained in an accessible
     *         Groovy project. False otherwise
     */
    protected boolean isProblemInGroovyProject(IInvocationContext context) {
        if (context == null) {
            return false;
        }
        return isContentInGroovyProject(context.getCompilationUnit());
    }

    /**
     * True if the problem is contained in an accessible (open and existing)
     * Groovy project in the workspace. False otherwise.
     * 
     * @param unit
     *            compilation unit containing the resource with the problem
     * @return true if and only if the problem is contained in an accessible
     *         Groovy project. False otherwise
     */
    protected boolean isContentInGroovyProject(ICompilationUnit unit) {

        if (unit != null) {
            IResource resource = unit.getResource();
            if (resource != null) {
                IProject project = resource.getProject();
                if (project != null && project.isAccessible()
                        && GroovyNature.hasGroovyNature(project)) {
                    return true;
                }
            }
        }
        return false;
    }


}
