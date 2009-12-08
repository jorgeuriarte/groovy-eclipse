/*******************************************************************************
 * Copyright (c) 2009 SpringSource and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Andrew Eisenberg - initial API and implementation
 *******************************************************************************/

package org.codehaus.groovy.eclipse.codeassist.tests;

import junit.framework.Test;
import junit.framework.TestSuite;


/**
 * @author Andrew Eisenberg
 * @created Jun 3, 2009
 *
 */
public class AllCompletionTests {
    public static Test suite() throws Exception {
        TestSuite suite = new TestSuite("Run of all Completion Tests");
        suite.addTestSuite(DefaultGroovyMethodCompletionTests.class);
        suite.addTestSuite(LocalVariableCompletionTests.class);
        suite.addTestSuite(InferencingCompletionTests.class);
        suite.addTestSuite(FieldCompletionTests.class);
        suite.addTestSuite(MethodCompletionTests.class);
        suite.addTestSuite(TypeCompletionTests.class);
        suite.addTestSuite(GroovyLikeCompletionTests.class);
        
        suite.addTestSuite(OtherCompletionTests.class);
        
        return suite;
    }
}
