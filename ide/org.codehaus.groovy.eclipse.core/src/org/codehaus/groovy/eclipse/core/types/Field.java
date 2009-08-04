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
package org.codehaus.groovy.eclipse.core.types;

public class Field extends Member {
	public Field(String signature, int modifiers, String name, ClassType declaringClass) {
		this(signature, modifiers, name, declaringClass, false);
	}
	
	public Field(String signature, int modifiers, String name, ClassType declaringClass, boolean inferred) {
		super(signature, modifiers, name, declaringClass, inferred);
	}
	
	public int getType() {
		return Type.FIELD;
	}

	public String toString() {
		return "Field:" + name + " " + signature + " - " + declaringClass;
	}
}