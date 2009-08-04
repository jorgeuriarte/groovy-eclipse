/* 
 * Copyright (C) 2007, 2009 Martin Kempf, Reto Kleeb, Michael Klenk
 *
 * IFS Institute for Software, HSR Rapperswil, Switzerland
 * http://ifs.hsr.ch/
 *
 */
package org.codehaus.groovy.eclipse.refactoring.core;

import org.codehaus.groovy.eclipse.refactoring.core.documentProvider.IGroovyDocumentProvider;
import org.codehaus.groovy.eclipse.refactoring.core.utils.StatusHelper;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.RefactoringStatusEntry;


/**
 * Info Object to provide the selection, editor and root node of the file to the Refactoring Class
 * The Refactoring wizard, can also access to this info object to exchange information with
 * the currently running Refactoring
 * @author Michael Klenk
 *
 */
abstract public class RefactoringProvider {

	protected UserSelection selection;
	
	public RefactoringProvider(UserSelection selecion) {
		this.selection = selecion;
	}

	public UserSelection getSelection() {
		return selection;
	}

	public void setSelection(UserSelection selection) {
		this.selection = selection;
	}
	
	public abstract void addInitialConditionsCheckStatus(RefactoringStatus status);
	
	protected void addStatusEntries(RefactoringStatus status, IStatus stateDuplicateName) {
		for (RefactoringStatusEntry refState : StatusHelper.convertStatus(stateDuplicateName).getEntries()){
			status.addEntry(refState);
		}
	}
	
	public abstract RefactoringStatus checkFinalConditions(
			IProgressMonitor pm) throws CoreException,
			OperationCanceledException;

	public abstract RefactoringStatus checkInitialConditions(
			IProgressMonitor pm) throws CoreException,
			OperationCanceledException;

	public abstract GroovyChange createGroovyChange(IProgressMonitor pm)
			throws CoreException, OperationCanceledException;
	
	public abstract IGroovyDocumentProvider getDocumentProvider();
}