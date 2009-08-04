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

package org.codehaus.groovy.eclipse.test.debug;

import java.io.InputStream;
import java.net.URL;

import org.apache.commons.io.IOUtils;
import org.codehaus.groovy.eclipse.core.model.GroovyRuntime;
import org.codehaus.groovy.eclipse.debug.ui.ToggleBreakpointAdapter;
import org.codehaus.groovy.eclipse.editor.GroovyEditor;
import org.codehaus.groovy.eclipse.test.Activator;
import org.codehaus.groovy.eclipse.test.EclipseTestCase;
import org.codehaus.groovy.eclipse.test.SynchronizationUtils;
import org.eclipse.core.resources.IFile;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IBreakpointManager;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.groovy.core.util.ReflectionUtils;
import org.eclipse.jdt.internal.debug.ui.actions.ActionDelegateHelper;
import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.TextSelection;

/**
 * @author Andrew Eisenberg
 * @created Jul 24, 2009
 *
 */
public class DebugBreakpointsTests extends EclipseTestCase {
    private static final String BREAKPOINT_SCRIPT_NAME = "BreakpointTesting.groovy";
    
    private ToggleBreakpointAdapter adapter;
    
    private ICompilationUnit unit;
    
    private GroovyEditor editor;
    
    private String text;
    
    @Override
    protected void setUp() throws Exception {
        super.setUp();
         GroovyRuntime.addGroovyRuntime(testProject.getProject());

         InputStream input = null;
         final URL url = Activator.bundle().getEntry(
                 "/testData/groovyfiles/" + BREAKPOINT_SCRIPT_NAME);
         try {
             input = url.openStream();
             IFile file = testProject.createGroovyTypeAndPackage("shapes",
                     BREAKPOINT_SCRIPT_NAME, input);
             
             unit = JavaCore.createCompilationUnitFrom(file);
         } finally {
             IOUtils.closeQuietly(input);
         }
         
         try {
             input = url.openStream();
             text = IOUtils.toString(input);
         } finally {
             IOUtils.closeQuietly(input);
         }
         adapter = new ToggleBreakpointAdapter();
         
         editor = (GroovyEditor) EditorUtility.openInEditor(unit);
         
         ReflectionUtils.setPrivateField(ActionDelegateHelper.class, "fTextEditor", ActionDelegateHelper.getDefault(), editor);
         
         unit.becomeWorkingCopy(null);
         unit.makeConsistent(null);
         SynchronizationUtils.joinBackgroudActivities();
    }
    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        unit.discardWorkingCopy();
        editor.close(false);
        SynchronizationUtils.joinBackgroudActivities();
    }
    
    public void testBreakpointInScript1() throws Exception {
        doBreakpointTest(1);
    }
    
    public void testBreakpointInScript2() throws Exception {
        doBreakpointTest(2);
    }

    public void testBreakpointInScript3() throws Exception {
        doBreakpointTest(3);
    }

    public void testBreakpointInScript4() throws Exception {
        doBreakpointTest(4);
    }

    public void testBreakpointInScript5() throws Exception {
        doBreakpointTest(5);
    }

    public void testBreakpointInScript6() throws Exception {
        doBreakpointTest(6);
    }

    public void testBreakpointInScript7() throws Exception {
        doBreakpointTest(7);
    }

    public void testBreakpointInScript8() throws Exception {
        doBreakpointTest(8);
    }

    public void testBreakpointInScript9() throws Exception {
        doBreakpointTest(9);
    }

    public void testBreakpointInScript10() throws Exception {
        doBreakpointTest(10);
    }

    public void testBreakpointInScript11() throws Exception {
        doBreakpointTest(11);
    }

    public void testBreakpointInScript12() throws Exception {
        doBreakpointTest(12);
    }

    public void testBreakpointInScript13() throws Exception {
        doBreakpointTest(13);
    }

    private void doBreakpointTest(int i) throws Exception {
        ITextSelection selection = new TextSelection(text.indexOf("// " + i), 3);
        boolean canToggle = adapter.canToggleLineBreakpoints(editor, selection);
        assertTrue("Should be able to toggle breakpoint at section " + i, canToggle);
        
        int initialNumBreakpoints;
        IBreakpointManager breakpointManager = DebugPlugin.getDefault().getBreakpointManager();
        IBreakpoint[] breakpoints = breakpointManager.getBreakpoints();
        initialNumBreakpoints = breakpoints.length;
        try {
            adapter.toggleLineBreakpoints(editor, selection);
            SynchronizationUtils.joinBackgroudActivities();
            
        } finally {
            IBreakpoint[] newBreakpoints = breakpointManager.getBreakpoints();
            assertEquals("Unexpected number of breakpoints", initialNumBreakpoints+1, newBreakpoints.length);
            for (IBreakpoint breakpoint : newBreakpoints) {
                breakpointManager.removeBreakpoint(breakpoint, true);
            }
            assertEquals("Should have deleted all breakpoints", 0, breakpointManager.getBreakpoints().length);
        }
    }
}