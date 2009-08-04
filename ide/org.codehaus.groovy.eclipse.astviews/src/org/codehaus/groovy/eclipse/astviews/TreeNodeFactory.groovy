/*******************************************************************************
 * Copyright (c) 2007, 2009 Codehaus.org, SpringSource, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Andrew Eisenberg - modified for Groovy Eclipse 2.0
 *******************************************************************************/
package org.codehaus.groovy.eclipse.astviews
import org.codehaus.groovy.ast.ASTNode
import org.codehaus.groovy.eclipse.core.util.JVM1_4Util

/**
 * Factory to create tree nodes.
 * @author emp
 */
class TreeNodeFactory {
	static forceDefaultNode = [
		org.codehaus.groovy.ast.CompileUnit,
		org.codehaus.groovy.ast.DynamicVariable,
		org.codehaus.groovy.ast.Parameter,
		org.codehaus.groovy.ast.Variable,
		org.codehaus.groovy.ast.VariableScope,
		org.codehaus.groovy.control.SourceUnit,
	]
	
	static NULL_VALUE = new Object()
	
	static ITreeNode createTreeNode(ITreeNode parent, Object value, String displayName) {
		if (value == null) {
			value = NULL_VALUE
		}
		// The displayName attributes are set after the constructor in the code below because
		//  displayName relies on the value object being set before it.  Since groovy does not 
		//  honor the order of the attributes in the constructor declaration, value is null when
		//  setDisplayName() is called and the ASTViewer becomes frustrating.
		if (value instanceof ASTNode || forceDefaultNode.contains(value.class)) {
			def node = new DefaultTreeNode( parent:parent, value:value )
		    node.displayName = displayName
		    return node
		} else if (value instanceof Object[] || value instanceof List) {
			def node = new CollectionTreeNode( parent:parent, value:value )
			node.displayName = displayName
			return node
		} else if (value instanceof Map) {
			def node = new MapTreeNode( parent:parent, value:value )
			node.displayName = displayName
			return node
		} else {
			def node = new AtomTreeNode( parent:parent, value:value )
			node.displayName = displayName
			return node
		}
	}
}

class StringUtil {
	static String toString(Object o) {
		return o.toString()
	}
	
	static String toString(Class cls) {
		// If o is Integer, then Integer.toString() tries to call either of the static toString() methods of Integer.
		// There may be other classes with static toString(args) methods.
		// This is the same code as in Class.toString()
        return (cls.isInterface() ? "interface " : (cls.isPrimitive() ? "" : "class ")) + cls.getName();
	}
}

abstract class TreeNode implements ITreeNode {
	static final ITreeNode[] NO_CHILDREN = new ITreeNode[0]

	ITreeNode parent
	Object value
	String displayName
	Boolean leaf
	ITreeNode[] children = null
	
	void setDisplayName(String name) {
		def mappedName = MapTo.names(value)
		if (mappedName) { name = "$name - $mappedName" }
		displayName = name
	}
	
	ITreeNode[] getChildren() {
		if (children == null) {
			children = loadChildren()
			if (children == null) {
				children = NO_CHILDREN
			}
		}
		return children
	}
	
	boolean isLeaf() {
		if (leaf == null) {
			if (children == null) {
				children = loadChildren()
			}
			leaf = children.length == 0
		}
		return leaf
	}
	
	abstract ITreeNode[] loadChildren()
}

class DefaultTreeNode extends TreeNode {	
	ITreeNode[] loadChildren() {
		def methods = value.class.getMethods()
		methods = methods?.findAll { it.name.startsWith('get') && it.getParameterTypes().length == 0 }
		def children = methods?.collect { method ->
			def name = method.name[3..-1]
			name = name[0].toLowerCase() + name[1..-1]
			try {
				return TreeNodeFactory.createTreeNode(this, value."${method.name}"(), name)
			} catch (org.codehaus.groovy.GroovyBugError e) {
//				 Some getters are not for us.
				return null
			} catch (NullPointerException e) {
				// For some reason ClassNode.getAbstractMethods() has a problem - ClassNode.superclass is null.
				return null
			}
		}
		// Make sure there are no nulls from the above exception handling.
		children = children?.findAll { it != null }
		if (children == null) {
			children = NO_CHILDREN
		}

		return children
	}
}

// This includes object arrays.
class CollectionTreeNode extends TreeNode {
	ITreeNode[] loadChildren() {
		return value.collect {
			def name = StringUtil.toString(it)
			if (name.indexOf('@') != -1) {
				name = JVM1_4Util.getSimpleName(it.class)
			}
			TreeNodeFactory.createTreeNode(this, it, name)
		} as ITreeNode[]
	}
}

class MapTreeNode extends TreeNode {
	ITreeNode[] loadChildren() {
		return value.collect { k, v -> TreeNodeFactory.createTreeNode(this, v, k) } as ITreeNode[]
	}
}

class AtomTreeNode implements ITreeNode {
	ITreeNode parent
	Object value
	String displayName

	boolean isLeaf() { return true }
	
	ITreeNode[] getChildren() { return NO_CHILDREN }
	
	void setDisplayName(String name) {
		def mappedName = MapTo.names(value)
		if (mappedName) { name = "$name - $mappedName" }
		
		if (value instanceof String) {
			displayName = "$name : '${StringUtil.toString(value)}'".toString()
		} else {
			def valueName = StringUtil.toString(value)
			if (valueName.indexOf('@') != -1) {
				valueName = JVM1_4Util.getSimpleName(value.class)
			}
			displayName = "$name : ${StringUtil.toString(valueName)}".toString()
		}
	}
}