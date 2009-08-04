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
package org.codehaus.groovy.eclipse.core.util;
import static org.codehaus.groovy.eclipse.core.GroovyCore.logException;
import static org.codehaus.groovy.eclipse.core.util.ListUtil.newList;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;

/**
 * Various shared core utilities.
 * 
 * @author empovazan
 */
public class CoreUtils 
{
	/**
	 * @param file The file containing text for which to create a line to offset mapping.
	 * @return A list of Integer offsets into the file. Note that index 0 == line 1 offset == 0.
	 * @throws CoreException
	 * @throws IOException
	 */
	public static List< Integer > createLineToOffsetMapping( final IFile file ) 
	throws IOException, CoreException 
	{
	    final List< Integer > offsets = newList();
        if( file == null )
            return offsets;
        int offset = 0;
        int line = 1;
        final BufferedReader reader = new BufferedReader( new InputStreamReader( file.getContents(), file.getCharset() ) );
        try 
        {
            offsets.add( new Integer( offset ) );
            int ch;
            while( ( ch = reader.read() ) != -1 ) 
            {
                ++offset;
                if( ch == '\r' ) 
                {
                    ch = reader.read();
                    ++offset;
                    if( ch == '\n' ) 
                        offsets.add( new Integer( offset ) );
                    else 
                        offsets.add( new Integer( offset - 1 ) );
                    ++line;
                }
                else if( ch == '\n' ) 
                {
                    offsets.add( new Integer( offset ) );
                    ++line;
                }
            }
        }
        finally 
        {
            reader.close();
        }
        return offsets;
    }
	/**
	 * @param text Text for which to create a line to offset mapping.
	 * @return A list of Integer offsets into the file. Note that index 0 == line 1 offset == 0.
	 */
	public static List< Integer > createLineToOffsetMapping( final String text ) 
	{
	    final List< Integer > offsets = newList();
        int offset = 0;
        int line = 1;
        final StringReader reader = new StringReader( text );
        try 
        {
            offsets.add( new Integer( offset ) );
            int ch;
            while( ( ch = reader.read() ) != -1 )
            {
                ++offset;
                if( ch == '\r' ) 
                {
                    ch = reader.read();
                    ++offset;
                    if( ch == '\n' )
                        offsets.add( new Integer( offset ) );
                    else
                        offsets.add( new Integer( offset - 1 ) );
                    ++line;
                }
                else if( ch == '\n' ) 
                {
                    offsets.add( new Integer( offset ) );
                    ++line;
                }
            }
        }
        catch( final IOException e ) 
        {
            logException( "Internal error, please report", e );
            throw new RuntimeException( e );
        }
        finally 
        {
            reader.close();
        }

        return offsets;
    }

}