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
package org.codehaus.groovy.eclipse.core.context.impl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import junit.framework.TestCase;
import org.codehaus.groovy.antlr.GroovySourceAST;
import org.codehaus.groovy.ast.ModuleNode;
import org.codehaus.groovy.eclipse.core.CoreTestsActivator;
import org.codehaus.groovy.eclipse.core.compiler.GroovyCompiler;
import org.codehaus.groovy.eclipse.core.compiler.GroovyCompilerConfigurationBuilder;
import org.codehaus.groovy.eclipse.core.compiler.IGroovyCompilationReporter;
import org.codehaus.groovy.eclipse.core.compiler.IGroovyCompiler;
import org.codehaus.groovy.eclipse.core.compiler.IGroovyCompilerConfiguration;
import org.codehaus.groovy.eclipse.core.context.ISourceCodeContext;
import org.codehaus.groovy.eclipse.core.impl.StringSourceBuffer;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;

/**
 * Tests that the various contexts are created along with their ASTNode paths.
 * <p>
 * The test relies on a test file that has special comment tags. These are parsed from the test file before testing
 * begins.
 * 
 * @author empovazan
 */
public abstract class GroovyContextFactoryTests extends TestCase {
	private static final String TEST_DATA_DIR = "testData/";
	
	public static final String MODULE = ISourceCodeContext.MODULE;

	public static final String MODULE_SCOPE = ISourceCodeContext.MODULE_SCOPE;

	public static final String CLASS = ISourceCodeContext.CLASS;

	public static final String CLASS_SCOPE = ISourceCodeContext.CLASS_SCOPE;

	public static final String CONSTRUCTOR_SCOPE = ISourceCodeContext.CONSTRUCTOR_SCOPE;

	public static final String CONSTRUCTOR_PARAMETERS = ISourceCodeContext.CONSTRUCTOR_PARAMETERS;

	public static final String METHOD_SCOPE = ISourceCodeContext.METHOD_SCOPE;

	public static final String METHOD_PARAMETERS = ISourceCodeContext.METHOD_PARAMETERS;

	public static final String CLOSURE_SCOPE = ISourceCodeContext.CLOSURE_SCOPE;

	/**
	 * Line column container.
	 */
	static class RegionInfo {
		final String testMethodName;
		
		IRegion region;
		
		final int startCol;
		final int endCol;
		
		final int line;

		RegionInfo(String testMethod, int line, int startCol, int endCol) {
			this.testMethodName = testMethod;
			this.startCol = startCol;
			this.endCol = endCol;
			this.line = line;
		}

		@Override
        public String toString() {
			return testMethodName + ":" + region.getOffset() + ":" + region.getLength();
		}
	}

	/**
	 * Reporter to get the AST from.
	 */
	static class TestCompilationReporter implements IGroovyCompilationReporter {
		public ModuleNode moduleNode;

		public void beginReporting() {
		}

		public void beginReporting(String fileName) {
		}

		public void compilationError(String fileName, int line, int startCol, int endCol, String message,
				String stackTrace) {
			throw new IllegalStateException(message + "\n" + stackTrace);
		}

		public void endReporting() {
		}

		public void endReporting(String fileName) {
		}

		public void generatedAST(String fileName, ModuleNode moduleNode) {
			this.moduleNode = moduleNode;
		}

		public void generatedCST(String fileName, GroovySourceAST cst) {
		}

		public void generatedClasses(String fileName, String[] classNames, String[] classFilePaths) {
		}
	}

	// The source code with tabs replaces with spaces.
	String sourceCode;

	// Mapping from test case method name to an array of line/column info.
	Map<String, RegionInfo[]> mapTestCaseNameToLineColInfos;

	void checkContextCount(String testName, Class countContextClass) {
		StringSourceBuffer buffer = new StringSourceBuffer(sourceCode);
		SourceCodeContextFactory factory = new SourceCodeContextFactory();
		ModuleNode moduleNode = compileTestSource();
		RegionInfo[] infos = mapTestCaseNameToLineColInfos.get(getName());
		int countOfContexts = 0;
		for (int i = 0; i < infos.length; ++i) {
			ISourceCodeContext[] contexts = factory.createContexts(buffer, moduleNode, infos[i].region);
			for (int ixCtx = 0; ixCtx < contexts.length; ++ixCtx) {
				if (countContextClass.isAssignableFrom(contexts[ixCtx].getClass())) {
					System.out.println(getName() + ": " + contexts[ixCtx]);
					++countOfContexts;
				}
			}
		}
		assertEquals(infos.length, countOfContexts);
	}

	void checkNoContext(String testName, Class excludeContextClass) {
		StringSourceBuffer buffer = new StringSourceBuffer(sourceCode);
		SourceCodeContextFactory factory = new SourceCodeContextFactory();
		ModuleNode moduleNode = compileTestSource();
		RegionInfo[] infos = mapTestCaseNameToLineColInfos.get(getName());
		for (int i = 0; i < infos.length; ++i) {
			ISourceCodeContext[] contexts = factory.createContexts(buffer, moduleNode, infos[i].region);

			for (int ixCtx = 0; ixCtx < contexts.length; ++ixCtx) {
				assertFalse(contexts[ixCtx].getClass().isAssignableFrom(excludeContextClass));
			}
		}
	}
	
	void checkContextPath(String testName, String[] ids) {
		StringSourceBuffer buffer = new StringSourceBuffer(sourceCode);
		SourceCodeContextFactory factory = new SourceCodeContextFactory();
		ModuleNode moduleNode = compileTestSource();
		RegionInfo[] infos = mapTestCaseNameToLineColInfos.get(getName());
		for (int i = 0; i < infos.length; ++i) {
			ISourceCodeContext[] contexts = factory.createContexts(buffer, moduleNode, infos[i].region);
			assertEquals(ids.length, contexts.length);
			for (int ixCtx = 0; ixCtx < contexts.length; ++ixCtx) {
				assertEquals(ids[ixCtx], contexts[ixCtx].getId());
			}
		}
	}

	private ModuleNode compileTestSource() {
		ByteArrayInputStream is = new ByteArrayInputStream(sourceCode.getBytes());
		TestCompilationReporter reporter = new TestCompilationReporter();
		IGroovyCompiler compiler = new GroovyCompiler();
		IGroovyCompilerConfiguration config = new GroovyCompilerConfigurationBuilder().buildAST().done();
		compiler.compile("ContextTestCode.groovy", is, config, reporter);
		return reporter.moduleNode;
	}
	
	protected void setSourceCode(String sourceCode) throws Exception {
		URL url = CoreTestsActivator.bundle().getEntry(
				TEST_DATA_DIR + sourceCode);
		InputStream is = url.openStream();
		getClass().getResourceAsStream(sourceCode);
		LineNumberReader reader = new LineNumberReader(new InputStreamReader(is));
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		PrintWriter writer = new PrintWriter(output);

		String line = reader.readLine();
		String spaces = getTabSpaces(line);
		line = line.replaceAll("\\t", spaces);
		writer.println(line);
		while ((line = reader.readLine()) != null) {
			line = line.replaceAll("\\t", spaces);
			writer.println(line);
		}

		reader.close();
		writer.close();

		this.sourceCode = output.toString();

		mapTestCaseNameToLineColInfos = mapTestCaseNameToLineColInfos(this.sourceCode);
	}

	private String getTabSpaces(String line) {
		String tabSpacing = line.substring(0, "//#TAB_SPACING:4".length());
		int spacing = Integer.parseInt(tabSpacing.substring(tabSpacing.indexOf(':') + 1));
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < spacing; ++i) {
			sb.append(" ");
		}
		return sb.toString();
	}

	private Map<String, RegionInfo[]> mapTestCaseNameToLineColInfos(String sourceCode) throws Exception {
		Map<String, List<RegionInfo>> map = new HashMap<String, List<RegionInfo>>();

		LineNumberReader reader = new LineNumberReader(new StringReader(sourceCode));
		String line;
		int offset = 0;
		while ((line = reader.readLine()) != null) {
			List<RegionInfo> infos = parseLineColInfos(line, reader.getLineNumber());
			if (infos != null) {
				for (Iterator<RegionInfo> iter = infos.iterator(); iter.hasNext();) {
					RegionInfo info = (RegionInfo) iter.next();
					getLineColInfos(map, info.testMethodName).add(info);
				}
			}
			offset += (line.length() + 1); // +1 for the newline.  but we don't handle \r\n 
		}
		
		IDocument doc = new Document(sourceCode);
		for (List<RegionInfo> listRegions : map.values()) {
			for (RegionInfo info : listRegions) {
				info.region = new Region(doc.getLineInformation(info.line-1).getOffset() + info.startCol - 1, 
						info.endCol - info.startCol);
			}
		}

		return convertLineColInfosToArrays(map);
	}

	private Map<String, RegionInfo[]> convertLineColInfosToArrays(Map<String, List<RegionInfo>> origMap) {
		Map<String, RegionInfo[]> newMap = new HashMap<String, RegionInfo[]>();
		Set<String> set = origMap.keySet();
		for (Iterator<String> iter = set.iterator(); iter.hasNext();) {
			String testMethodName = iter.next();
			List<RegionInfo> infos = origMap.get(testMethodName);
			newMap.put(testMethodName, infos.toArray(new RegionInfo[infos.size()]));
		}
		return newMap;
	}

	private List<RegionInfo> getLineColInfos(Map<String, List<RegionInfo>> map, String testMethod) {
		List<RegionInfo> currentInfos = map.get(testMethod);
		if (currentInfos == null) {
			currentInfos = new ArrayList<RegionInfo>();
			map.put(testMethod, currentInfos);
		}
		return currentInfos;
	}

	/**
	 * Given a string like this:<br>
	 * "// #testInModuleBody,1,17,23 #testNotInModuleBody,24 #testNotInClass,23 #testInClass,24"<br>
	 * parse the LineColInfo from the string.
	 * 
	 * @param line
	 *            The line text.
	 * @param currentOffset
	 *            The current offset.
	 * @return A List of RegionInfo.
	 */
	private List<RegionInfo> parseLineColInfos(String l, int lineNumber) {
		List<RegionInfo> infos = null;
		String line = l.trim();
		if (line.startsWith("//")) {
			line = line.substring(2);
			infos = new ArrayList<RegionInfo>();
			Pattern pattern = Pattern.compile("(?:\\s*#(test\\w+(?:,\\d+)+))");
			Matcher matcher = pattern.matcher(line);
			while (matcher.find()) {
				String match = matcher.group(1);
				String[] parts = match.split(",");
				String testMethodName = parts[0];
				for (int i = 1; i < parts.length; ++i) {
					// +1 as the comment test tag refers to the line under it.
					infos.add(new RegionInfo(testMethodName, lineNumber+1, 
							Integer.parseInt(parts[i]), 
							Integer.parseInt(parts[i])));
				}
			}
		}
		return infos;
	}
}