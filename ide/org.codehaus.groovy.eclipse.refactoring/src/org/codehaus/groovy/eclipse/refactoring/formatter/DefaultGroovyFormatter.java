/* 
 * Copyright (C) 2007, 2009 Martin Kempf, Reto Kleeb, Michael Klenk
 *
 * IFS Institute for Software, HSR Rapperswil, Switzerland
 * http://ifs.hsr.ch/
 *
 */
package org.codehaus.groovy.eclipse.refactoring.formatter;

import antlr.Token;
import antlr.TokenStream;
import antlr.TokenStreamException;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Vector;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.codehaus.groovy.antlr.parser.GroovyLexer;
import org.codehaus.groovy.antlr.parser.GroovyTokenTypes;
import org.codehaus.groovy.ast.ASTNode;
import org.codehaus.groovy.ast.AnnotatedNode;
import org.codehaus.groovy.ast.ModuleNode;
import org.codehaus.groovy.ast.expr.VariableExpression;
import org.codehaus.groovy.ast.stmt.ExpressionStatement;
import org.codehaus.groovy.ast.stmt.Statement;
import org.codehaus.groovy.eclipse.refactoring.core.utils.ASTTools;
import org.codehaus.groovy.eclipse.refactoring.core.utils.astScanner.ASTNodeInfo;
import org.codehaus.groovy.eclipse.refactoring.core.utils.astScanner.ASTScanner;
import org.codehaus.groovy.eclipse.refactoring.core.utils.astScanner.predicates.IncludesClosurePredicate;
import org.codehaus.groovy.eclipse.refactoring.core.utils.astScanner.predicates.SourceCodePredicate;
import org.codehaus.groovy.eclipse.refactoring.ui.GroovyRefactoringMessages;
import org.eclipse.core.runtime.Preferences;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.text.edits.UndoEdit;

/**
 * @author Mike Klenk mklenk@hsr.ch
 * 
 */
public class DefaultGroovyFormatter extends GroovyFormatter {

	protected FormatterPreferences pref;
	private ModuleNode rootNode;
	private Document formattedDocument;
	private final boolean indentendOnly;
	public int formatOffset, formatLength;

	private Vector<Token> tokens;
	private Vector<Vector<Token>> tokenLines;
	private int indentationLevel = 0;

	/**
	 * Default Formatter for the Groovy-Eclipse Plugin
	 * @param sel The current selection of the Editor
	 * @param doc The Document which should be formatted
	 * @param preferences default Plugin preferences, or selfmade preferences
	 * @param indentendOnly if true, the code will only be indentended not formatted
	 */
	public DefaultGroovyFormatter(ITextSelection sel, IDocument doc,
			Preferences preferences, boolean indentendOnly) {
		super(sel, doc);
		this.indentendOnly = indentendOnly;
		pref = new FormatterPreferences(preferences);
		if (selection.getLength() != 0) {
			formatOffset = selection.getOffset();
			formatLength = selection.getLength();

		} else {
			formatOffset = 0;
			formatLength = document.getLength();
		}
	}
	public DefaultGroovyFormatter(IDocument doc,
			Preferences preferences, int indentationLevel) {
		this(new TextSelection(0,0),doc,preferences,true);
		this.indentationLevel = indentationLevel;
	}


	private void initCodebase() throws Exception {
		
		System.out.println(formattedDocument.get());
		
		tokens = new Vector<Token>();
		tokenLines = new Vector<Vector<Token>>();
		rootNode = ASTTools.getASTNodeFromSource(formattedDocument.get());
		if(rootNode == null) {
			throw new Exception(GroovyRefactoringMessages.FormattingAction_RootNode_Errors);
		}

			InputStream input = new ByteArrayInputStream(formattedDocument.get().getBytes());
			GroovyLexer lexer = new GroovyLexer(input);	
			lexer.setWhitespaceIncluded(true);
			TokenStream stream = (TokenStream) lexer.plumb();
			
			Token token = null;
			Vector<Token> line = new Vector<Token>();
			while ((token = stream.nextToken()).getType() != GroovyTokenTypes.EOF) {
				if (token.getType() != GroovyTokenTypes.WS) {
					// Ignore Tokens inside a String
					if(token.getType() == GroovyTokenTypes.STRING_CTOR_START) {
						tokens.add(token);
						while((token = stream.nextToken()).getType() != GroovyTokenTypes.STRING_CTOR_END) {
						}
					}
					tokens.add(token);
					line.add(token);
					if(token.getType() == GroovyTokenTypes.NLS) {
						tokenLines.add(line);
						line = new Vector<Token>();
					}
				}
			}
			// Adding last Line with EOF at End
			tokens.add(token);
			line.add(token);
			tokenLines.add(line);
	}
	
	

	@Override
	public TextEdit format() {
		formattedDocument = new Document(document.get());
		try {
			if (!indentendOnly) {
				initCodebase();
				GroovyBeautifier beautifier = new GroovyBeautifier(this, pref);
				int lengthBefore = formattedDocument.getLength();
				beautifier.getBeautifiEdits().apply(formattedDocument);
				int lengthAfter = formattedDocument.getLength();
				formatLength += lengthAfter - lengthBefore;
			}
			
			initCodebase();
			GroovyIndentation indent = new GroovyIndentation(this, pref, indentationLevel );
			UndoEdit undo2 = indent.getIndentationEdits().apply(formattedDocument);
			formatLength += undo2.getLength();

			
//			if (!indentendOnly) {
//				initCodebase();
//				GroovyLineWrapper linewrap = new GroovyLineWrapper(this, pref, indent.getLineIndentations());
//				UndoEdit undo3 = linewrap.getLineWrapEdits().apply(formattedDocument);
//				formatLength += undo3.getLength();
//			}

		} catch (Exception e) {
			throw new RuntimeException("Formatting could not be performed.\nMessage:" + e.getMessage() + "\nCause: " + e.getCause() + "\nType: " + e.getClass().getName());
		}
		if (!formattedDocument.get().equals(document.get()))
			return new ReplaceEdit(0, document.getLength(), formattedDocument
					.get());
        return new MultiTextEdit();
	}

	/**
	 * Searches in the corresponding AST if the given Token is a multiline
	 * statement. Trailling linefeeds and spaces will be ignored.
	 * 
	 * @param t
	 *            Token to search for
	 * @return returns true if the statement has more than one line in the
	 *         source code
	 */
	public boolean isMultilineStatement(Token t) {
		if (t != null) {
			ASTNode node = findCorrespondingNode(t);
			if (isMultilineNodeType(node)) {
				boolean closureTest = false;
				IncludesClosurePredicate cltest = new IncludesClosurePredicate(
						closureTest, t.getLine());
				node.visit(cltest);
				if (!cltest.getContainer()) {
					String text = ASTTools.getTextofNode(node,
							formattedDocument);
					Matcher m = Pattern.compile(".*(\n|\r\n|\r).*",
							Pattern.DOTALL).matcher(trimEnd(text));
					return m.matches();
				}
			}
		}
		return false;
	}

	/**
	 * Returns a string without spaces / line feeds at the end
	 * 
	 * @param s
	 * @return
	 */
	public String trimEnd(String s) {
		int len = s.length();

		while (len > 0) {
			String w = s.substring(len - 1, len);
			if (w.matches("\\s"))
				len--;
			else
				break;
		}
		return s.substring(0, len);
	}

	/**
	 * Tests if the ASTNode is a valid MultiNodeType an has multiple lines
	 * Statements, ClassNodes, MethodNodes and Variable Expressions are ignored
	 * @param node
	 * @return
	 */
	// FIXADE this is probably because LastLineNumber was being set incorrectly before
	private boolean isMultilineNodeType(ASTNode node) {
		if (node != null && node.getLineNumber() < node.getLastLineNumber()) {
			if (node instanceof ExpressionStatement)
				return true;
			if (node instanceof Statement)
				return false;
			if (node instanceof VariableExpression)
				return false;
			if (node instanceof AnnotatedNode)
				return false;

			return true;
		}
		return false;
	}

	/**
	 * Finding a AST Node corresponding to a Token ! Warning ! there can be
	 * found the wrong node if two nodes have the same line / col infos
	 * 
	 * @param t
	 *            Token for which the AST Node should be found.
	 * @return the Node with the start position of the token and the longest
	 *         length
	 */
	public ASTNode findCorrespondingNode(Token t) {
		ASTScanner scanner = new ASTScanner(rootNode, new SourceCodePredicate(t
				.getLine(), t.getColumn()), formattedDocument);
		scanner.startASTscan();
		Entry<ASTNode, ASTNodeInfo> found = null;
		if (scanner.hasMatches()) {
			for (Entry<ASTNode, ASTNodeInfo> e : scanner.getMatchedNodes()
					.entrySet()) {
				if (found == null
						|| (found.getValue().getLength() < e.getValue()
								.getLength()))
					found = e;
			}
		}
		if (found != null)
			return found.getKey();
        return null;
	}


	/**
	 * Return a token after many () if there is no opening {
	 * 
	 * @param i
	 *            position of the current token
	 * @return the token after the last closing )
	 */
	public Token getTokenAfterParenthesis(int index) {
	    int i = index;
		int countParenthesis = 1;
		while (tokens.get(i).getType() != GroovyTokenTypes.LPAREN)
			i++;
		i++;
		while (countParenthesis > 0) {
			switch (tokens.get(i).getType()) {
				case GroovyTokenTypes.LPAREN:
					countParenthesis++;
					break;
				case GroovyTokenTypes.RPAREN:
					countParenthesis--;
					break;
			}
			i++;
		}
		if (tokens.get(i).getType() == GroovyTokenTypes.LCURLY)
			return null;
        return getNextToken(i);
	}

	/**
	 * Returns a Sting of spaces / tabs accordung to the configuration
	 * 
	 * @param intentation
	 *            the actual indentation level
	 * @return
	 */
	public String getLeadingGap(int indent) {
	    int indentation = indent;
		StringBuilder gap = new StringBuilder();
		while (indentation > 0) {
			if (pref.useTabs)
				gap.append("\t");
			else {
				for (int i = 0; i < pref.tabSize; i++) {
					gap.append(" ");
				}
			}
			indentation--;
		}
		return gap.toString();
	}

	/**
	 * @return Returns the default newline for this document
	 * @throws BadLocationException
	 */
	public String getNewLine() {
		return formattedDocument.getDefaultLineDelimiter();
	}

	/**
	 * Returns the position of the next token in the collection of parsed tokens
	 * 
	 * @param currentPos
	 *            position of the actual cursor
	 * @param includingNLS
	 *            including newline tokens
	 * @return returns the position in the collection of tokens
	 */
	public int getPositionOfNextToken(int cPos, boolean includingNLS) {
	    int currentPos = cPos;
		int type;
		do {
			type = tokens.get(++currentPos).getType();
		} while ((type == GroovyTokenTypes.WS || (type == GroovyTokenTypes.NLS && !includingNLS))
				&& currentPos < tokens.size() - 1);
		return currentPos;
	}

	public Token getNextToken(int currentPos) {
		return tokens.get(getPositionOfNextToken(currentPos, false));
	}

	public Token getNextTokenIncludingNLS(int currentPos) {
		return tokens.get(getPositionOfNextToken(currentPos, true));
	}

	/**
	 * Returns the position of the previous token in the collection of parsed
	 * tokens
	 * 
	 * @param currentPos
	 *            position of the actual cursor
	 * @param includingNLS
	 *            including newline tokens
	 * @return returns the position in the collection of tokens
	 */
	public int getPositionOfPreviousToken(int cPos, boolean includingNLS) {
	    int currentPos = cPos;
		int type;
		do {
			type = tokens.get(--currentPos).getType();
		} while ((type == GroovyTokenTypes.NLS && !includingNLS)
				&& currentPos >= 0);
		return currentPos;
	}

	public Token getPreviousToken(int currentPos) {
		return tokens.get(getPositionOfPreviousToken(currentPos, false));
	}

	public Token getPreviousTokenIncludingNLS(int currentPos) {
		return tokens.get(getPositionOfPreviousToken(currentPos, true));
	}

	/**
	 * Return the offset of a given token in the active document
	 * @param token
	 * @return offset of the token
	 * @throws BadLocationException
	 */
	public int getOffsetOfToken(Token token) throws BadLocationException {
		return formattedDocument.getLineOffset(token.getLine() - 1)
				+ token.getColumn() - 1;
	}

	/**
	 * Return the offset of the end of the token text in the document
	 * @param token
	 * @return the offset in the document after the last character
	 * @throws BadLocationException
	 */
	public int getOffsetOfTokenEnd(Token token) throws BadLocationException {
		int offsetToken = getOffsetOfToken(token);
		int offsetNextToken = getOffsetOfToken(getNextTokenIncludingNLS(getPosOfToken(token)));
		String tokenWithGap = formattedDocument.get(offsetToken,
				offsetNextToken - offsetToken);
		return offsetToken + trimEnd(tokenWithGap).length();
	}
	
	/**
	 * Counts the length of a Token
	 * @param token
	 * @return length of the token
	 * @throws BadLocationException
	 */
	public int getTokenLength(Token token) throws BadLocationException {
		return getOffsetOfTokenEnd(token) - getOffsetOfToken(token);
	}
	
	public Vector<Vector<Token>> getLineTokens() {
		return tokenLines;
	}

	public int getPosOfToken(Token token) {
		return tokens.indexOf(token);
	}
	
	public int getPosOfToken(int tokenType, int line, int column, String tokenText) {
		for(int p = 0; p < tokens.size(); p++) {
			Token a = tokens.get(p);
			if(a.getType() == tokenType &&
					a.getColumn() == column &&
					a.getLine() == line &&
					a.getText().equals(tokenText))
				return p;
		}
		return -1;
	}
	
	public int getPosOfToken(int lineNumber, int columnNumber) {
		for(int p = 0; p < tokens.size(); p++) {
			Token a = tokens.get(p);
			if(a.getColumn() == columnNumber &&
					a.getLine() == lineNumber)
				return p;
		}
		return -1;
	}

	/**
	 * Get the active state of the document
	 * @return
	 */
	public IDocument getProgressDocument() {
		return formattedDocument;
	}
	
	public ModuleNode getProgressRootNode() {
		return rootNode;
	}

	public Vector<Token> getTokens() {
		return tokens;
	}

	public int getPosOfNextTokenOfType(int pClStart, int expectedType) {
	    int posClStart = pClStart;
		int type;
		do {
			type = tokens.get(++posClStart).getType();
		} while (type != expectedType);
		return posClStart;
	}



}