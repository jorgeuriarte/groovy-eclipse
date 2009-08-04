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
package org.codehaus.groovy.eclipse;
import java.io.IOException;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.codehaus.groovy.eclipse.editor.GroovyTextTools;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.templates.ContextTypeRegistry;
import org.eclipse.jface.text.templates.persistence.TemplateStore;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.editors.text.templates.ContributionContextTypeRegistry;
import org.eclipse.ui.editors.text.templates.ContributionTemplateStore;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * The main plugin class to be used in the desktop.
 */
public class GroovyPlugin extends AbstractUIPlugin {
	
	/**
	 * The single plugin instance
	 */
	private static GroovyPlugin plugin;
	
	/**
	 * Resource bundle unique id. 
	 */
	private ResourceBundle resourceBundle;
	
	static boolean trace;


	private GroovyTextTools textTools;
	

	public static final String PLUGIN_ID = "org.codehaus.groovy.eclipse.ui";

	public static final String GROOVY_TEMPLATE_CTX = "org.codehaus.groovy.eclipse.templates";
	
	private ContributionContextTypeRegistry fContextTypeRegistry;
	    
	private ContributionTemplateStore fTemplateStore;
	
	static {
		String value = Platform
				.getDebugOption("org.codehaus.groovy.eclipse/trace"); //$NON-NLS-1$
		if (value != null && value.equalsIgnoreCase("true")) //$NON-NLS-1$
			GroovyPlugin.trace = true;
	}

	/**
	 * The constructor.
	 */
	public GroovyPlugin() {
		super();
		plugin = this;
		try {
			resourceBundle = ResourceBundle
					.getBundle("org.codehaus.groovy.eclipse.TestNatureAndBuilderPluginResources");
		} catch (MissingResourceException x) {
			resourceBundle = null;
		}
    }

	/**
	 * @return Returns the plugin instance.
	 */
	public static GroovyPlugin getDefault() {
		return plugin;
	}

	/**
	 * Gets a string from the resource bundle
	 * 
	 * @param key
	 * @return Returns the string from the plugin's resource bundle, or 'key' if not
	 * found.
	 */
	public static String getResourceString(String key) {
		ResourceBundle bundle = GroovyPlugin.getDefault().getResourceBundle();
		try {
			return (bundle != null ? bundle.getString(key) : key);
		} catch (MissingResourceException e) {
			return key;
		}
	}
	
	public static Shell getActiveWorkbenchShell() {
		IWorkbenchWindow workBenchWindow= getActiveWorkbenchWindow();
		if (workBenchWindow == null)
			return null;
		return workBenchWindow.getShell();
	}

	/**
	 * Returns the active workbench window
	 * 
	 * @return the active workbench window
	 */
	public static IWorkbenchWindow getActiveWorkbenchWindow() {
		if (plugin == null)
			return null;
		IWorkbench workBench= plugin.getWorkbench();
		if (workBench == null)
			return null;
		return workBench.getActiveWorkbenchWindow();
	}

	/**
	 * @return Returns the plugin's resource bundle.
	 */
	public ResourceBundle getResourceBundle() {
		return resourceBundle;
	}

	/**
	 * Logs an exception
	 * 
	 * @param message The message to save.
	 * @param exception The exception to be logged.
	 */
	public void logException(String message, Exception exception) {
		log(IStatus.ERROR, message, exception);
	}
	
    /**
     * Logs a warning.
     * 
     * @param message The warning to log.
     */
    public void logWarning( final String message ){
    	log(IStatus.WARNING, message, null);
    }
    
	/**
	 * Logs an information message.
	 * 
	 * @param message The message to log.
	 */
	public void logTraceMessage(String message) {
		log(IStatus.INFO, message, null);
	}
	
	private void log(int severity, String message, Exception exception) {
		final IStatus status = new Status( severity, getBundle().getSymbolicName(), 0, message, exception );
        getLog().log( status );
	}

	/**
	 * @return Returns the dialogProvider.
	 */
//	public GroovyDialogProvider getDialogProvider() {
//		return dialogProvider;
//	}
//
//	/**
//	 * @param dialogProvider
//	 *            The dialogProvider to set.
//	 */
//	public void setDialogProvider(GroovyDialogProvider dialogProvider) {
//		this.dialogProvider = dialogProvider;
//	}

	public static void trace(String message) {
		if (trace) {
			getDefault().logTraceMessage("trace: " + message);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		textTools = new GroovyTextTools();
	}
	
	@Override
	public void stop(BundleContext context) throws Exception {
	    super.stop(context);
	    textTools.dispose();
	    textTools = null;
	}

	
	public GroovyTextTools getTextTools() {
        return textTools;
    }
	

    public IPreferenceStore getPreferenceStore() {
        return super.getPreferenceStore();
    }
    
	public IWorkbenchPage getActivePage() {
		IWorkbenchWindow window= getWorkbench().getActiveWorkbenchWindow();
		if (window == null) {
			return null;
		}
		return window.getActivePage();
	}
	
	public ContextTypeRegistry getContextTypeRegistry() {
		if (fContextTypeRegistry == null) {
			fContextTypeRegistry = new ContributionContextTypeRegistry();
			fContextTypeRegistry
				.addContextType(GROOVY_TEMPLATE_CTX);
		}
		return fContextTypeRegistry;
	}
	    
	public TemplateStore getTemplateStore() {
		if (fTemplateStore == null) {
			fTemplateStore = new ContributionTemplateStore(
					getContextTypeRegistry(),
					getDefault().getPreferenceStore(), "templates");
			try {
				fTemplateStore.load();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		return fTemplateStore;
	}
}