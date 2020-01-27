/*
 * Copyright (c) 2002-2017, Mairie de Paris
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  1. Redistributions of source code must retain the above copyright notice
 *     and the following disclaimer.
 *
 *  2. Redistributions in binary form must reproduce the above copyright notice
 *     and the following disclaimer in the documentation and/or other materials
 *     provided with the distribution.
 *
 *  3. Neither the name of 'Mairie de Paris' nor 'Lutece' nor the names of its
 *     contributors may be used to endorse or promote products derived from
 *     this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 * License 1.0
 */
package fr.paris.lutece.plugins.directory.business;

import fr.paris.lutece.portal.service.plugin.Plugin;
import fr.paris.lutece.portal.service.spring.SpringContextService;
import java.util.List;

/**
 * This class provides instances management methods (create, find, ...) for file objects
 */
public final class FileHome
{
    // Static variable pointed at the DAO instance
    private static IFileDAO _dao = SpringContextService.getBean( "directoryFileDAO" );

    /**
     * Private constructor - this class need not be instantiated
     */
    private FileHome( )
    {
    }

    /**
     * Creation of an instance of record file
     *
     * @param file
     *            The instance of the file which contains the informations to store
     * @param plugin
     *            the plugin
     *
     * @return the id of the file after creation
     *
     */
    public static int create( File file, Plugin plugin )
    {
        if ( file.getPhysicalFile( ) != null )
        {
            file.getPhysicalFile( ).setIdPhysicalFile( PhysicalFileHome.create( file.getPhysicalFile( ), plugin ) );
        }

        return _dao.insert( file, plugin );
    }

    /**
     * Update of file which is specified in parameter
     *
     * @param file
     *            The instance of the record file which contains the informations to update
     * @param plugin
     *            the Plugin
     *
     */
    public static void update( File file, Plugin plugin )
    {
        if ( file.getPhysicalFile( ) != null )
        {
            PhysicalFileHome.update( file.getPhysicalFile( ), plugin );
        }

        _dao.store( file, plugin );
    }

    /**
     * Delete the file whose identifier is specified in parameter
     *
     * @param nIdFile
     *            The identifier of the record file
     * @param plugin
     *            the Plugin
     */
    public static void remove( int nIdFile, Plugin plugin )
    {
        File file = FileHome.findByPrimaryKey( nIdFile, plugin );

        if ( file.getPhysicalFile( ) != null )
        {
            PhysicalFileHome.remove( file.getPhysicalFile( ).getIdPhysicalFile( ), plugin );
        }

        _dao.delete( nIdFile, plugin );
    }

    // /////////////////////////////////////////////////////////////////////////
    // Finders

    /**
     * Returns an instance of a file whose identifier is specified in parameter
     *
     * @param nKey
     *            The file primary key
     * @param plugin
     *            the Plugin
     * @return an instance of file
     */
    public static File findByPrimaryKey( int nKey, Plugin plugin )
    {
        return _dao.load( nKey, plugin );
    }

    /**
     * Purge the file associated to given id
     * 
     * @param file
     *            the file to purge
     * @param plugin
     *            The plugin
     */
    public static void purge( File file, Plugin plugin )
    {
        _dao.purge( file, plugin );
    }

    /**
     * Get the files list
     * 
     * @param plugin 
     * 				the plugin
     * @return the list of files
     */
    public static List<File> getFilesList( Plugin plugin )
    {
        return _dao.selectFilesList( plugin );
    }
}
