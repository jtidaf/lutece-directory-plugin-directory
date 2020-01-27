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
package fr.paris.lutece.plugins.directory.service.upload;

import fr.paris.lutece.plugins.blobstore.service.BlobStoreClientException;
import fr.paris.lutece.plugins.blobstore.service.IBlobStoreClientService;
import fr.paris.lutece.plugins.directory.business.EntryHome;
import fr.paris.lutece.plugins.directory.business.EntryTypeDownloadUrl;
import fr.paris.lutece.plugins.directory.business.File;
import fr.paris.lutece.plugins.directory.business.IEntry;
import fr.paris.lutece.plugins.directory.business.PhysicalFile;
import fr.paris.lutece.plugins.directory.business.PhysicalFileHome;
import fr.paris.lutece.plugins.directory.business.Record;
import fr.paris.lutece.plugins.directory.business.RecordField;
import fr.paris.lutece.plugins.directory.business.RecordFieldFilter;
import fr.paris.lutece.plugins.directory.business.RecordFieldHome;
import fr.paris.lutece.plugins.directory.service.DirectoryPlugin;
import fr.paris.lutece.plugins.directory.utils.DirectoryErrorException;
import fr.paris.lutece.plugins.directory.utils.DirectoryUtils;
import fr.paris.lutece.plugins.directory.utils.JSONUtils;
import fr.paris.lutece.plugins.directory.utils.UrlUtils;
import fr.paris.lutece.portal.service.fileupload.FileUploadService;
import fr.paris.lutece.portal.service.i18n.I18nService;
import fr.paris.lutece.portal.service.plugin.Plugin;
import fr.paris.lutece.portal.service.plugin.PluginService;
import fr.paris.lutece.portal.service.spring.SpringContextService;
import fr.paris.lutece.portal.service.util.AppException;
import fr.paris.lutece.portal.service.util.AppLogService;
import fr.paris.lutece.portal.web.upload.IAsynchronousUploadHandler;
import fr.paris.lutece.portal.web.upload.MultipartHttpServletRequest;
import fr.paris.lutece.util.filesystem.FileSystemUtil;
import fr.paris.lutece.util.filesystem.UploadUtil;

import net.sf.json.JSONObject;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemHeaders;
import org.apache.commons.lang.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * Handler for asynchronous uploads. The <code>jessionid</code> parameter should be the <strong>REAL</strong> session id, not the flash player one. The uploaded
 * files are deleted by SubForm when filling fields.
 *
 */
public class DirectoryAsynchronousUploadHandler implements IAsynchronousUploadHandler
{
    /** contains uploaded file items */
    /**
     * &lt;sessionId,&lt;fieldName,fileItems&gt;&gt;
     */
    public static Map<String, Map<String, List<FileItem>>> _mapAsynchronousUpload = new ConcurrentHashMap<String, Map<String, List<FileItem>>>( );
    private static final String BEAN_DIRECTORY_ASYNCHRONOUS_UPLOAD_HANDLER = "directory.asynchronousUploadHandler";
    private static final String PREFIX_ENTRY_ID = "directory_";

    // UPLOAD
    private static final String UPLOAD_SUBMIT_PREFIX = "_directory_upload_submit_directory_";
    private static final String UPLOAD_DELETE_PREFIX = "_directory_upload_delete_directory_";
    private static final String UPLOAD_CHECKBOX_PREFIX = "_directory_upload_checkbox_directory_";

    // PARAMETERS
    private static final String PARAMETER_BLOB_KEY = "blob_key";
    private static final String PARAMETER_BLOBSTORE = "blobstore";
    private static final String PARAMETER_PLUGIN_NAME = "plugin_name";
    private static final String PARAMETER_FIELD_NAME = "field_name";
    private static final String PARAMETER_JSESSION_ID = "jsessionid";

    // PROPERTIES
    private static final String PROPERTY_MESSAGE_ERROR_UPLOADING_FILE_SESSION_LOST = "directory.message.error.uploading_file.session_lost";
    private IBlobStoreClientService _blobStoreClientService;

    /**
     * Private constructor
     */
    private DirectoryAsynchronousUploadHandler( )
    {
    }

    /**
     * Get the handler
     * 
     * @return the handler
     */
    public static DirectoryAsynchronousUploadHandler getHandler( )
    {
        return SpringContextService.getBean( BEAN_DIRECTORY_ASYNCHRONOUS_UPLOAD_HANDLER );
    }

    /**
     * Set the blobstore client service
     * 
     * @param blobStoreClientService
     *            the blob store client service
     */
    public void setBlobStoreClientService( IBlobStoreClientService blobStoreClientService )
    {
        _blobStoreClientService = blobStoreClientService;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isInvoked( HttpServletRequest request )
    {
        return DirectoryPlugin.PLUGIN_NAME.equals( request.getParameter( PARAMETER_PLUGIN_NAME ) );
    }

    /**
     * Check if the service is available
     * 
     * @return true if the service is available, false otherwise
     */
    public boolean isBlobStoreClientServiceAvailable( )
    {
        return _blobStoreClientService != null;
    }

    /**
     * {@inheritDoc}
     * 
     * category CALLED_BY_JS (directoryupload.js)
     */
    @Override
    public void process( HttpServletRequest request, HttpServletResponse response, JSONObject mainObject, List<FileItem> listFileItemsToUpload )
    {
        // prevent 0 or multiple uploads for the same field
        if ( ( listFileItemsToUpload == null ) || listFileItemsToUpload.isEmpty( ) )
        {
            throw new AppException( "No file uploaded" );
        }

        String strIdSession = request.getParameter( PARAMETER_JSESSION_ID );

        if ( StringUtils.isNotBlank( strIdSession ) )
        {
            String strFieldName = request.getParameter( PARAMETER_FIELD_NAME );

            if ( StringUtils.isBlank( strFieldName ) )
            {
                throw new AppException( "id entry is not provided for the current file upload" );
            }

            initMap( strIdSession, strFieldName );

            // find session-related files in the map
            Map<String, List<FileItem>> mapFileItemsSession = _mapAsynchronousUpload.get( strIdSession );

            List<FileItem> fileItemsSession = mapFileItemsSession.get( strFieldName );

            if ( canUploadFiles( strFieldName, fileItemsSession, listFileItemsToUpload, mainObject, request.getLocale( ) ) )
            {
                fileItemsSession.addAll( listFileItemsToUpload );

                JSONObject jsonListFileItems = JSONUtils.getUploadedFileJSON( fileItemsSession );
                mainObject.accumulateAll( jsonListFileItems );
                // add entry id to json
                JSONUtils.buildJsonSuccess( strFieldName, mainObject );
            }
        }
        else
        {
            AppLogService.error( DirectoryAsynchronousUploadHandler.class.getName( ) + " : Session does not exists" );

            String strMessage = I18nService.getLocalizedString( PROPERTY_MESSAGE_ERROR_UPLOADING_FILE_SESSION_LOST, request.getLocale( ) );
            JSONUtils.buildJsonError( mainObject, strMessage );
        }
    }

    /**
     * Do upload a file in the blobstore webapp
     * 
     * @param strBaseUrl
     *            the base url
     * @param fileItem
     *            the file
     * @param strBlobStore
     *            the blobstore service name
     * @return the blob key of the uploaded file
     * @throws BlobStoreClientException
     *             Exception if there is an issue
     */
    public String doUploadFile( String strBaseUrl, FileItem fileItem, String strBlobStore ) throws BlobStoreClientException
    {
        if ( isBlobStoreClientServiceAvailable( ) )
        {
            return _blobStoreClientService.doUploadFile( strBaseUrl, fileItem, strBlobStore );
        }

        return StringUtils.EMPTY;
    }

    /**
     * Do remove a file from a given record and entry
     * 
     * @param record
     *            the record
     * @param entry
     *            the entry
     * @param strWSRestUrl
     *            the url of the WS rest
     * @throws BlobStoreClientException
     *             Exception if there is an issue
     */
    public void doRemoveFile( Record record, IEntry entry, String strWSRestUrl ) throws BlobStoreClientException
    {
        Plugin pluginDirectory = PluginService.getPlugin( DirectoryPlugin.PLUGIN_NAME );
        RecordFieldFilter recordFieldFilter = new RecordFieldFilter( );
        recordFieldFilter.setIdDirectory( record.getDirectory( ).getIdDirectory( ) );
        recordFieldFilter.setIdEntry( entry.getIdEntry( ) );
        recordFieldFilter.setIdRecord( record.getIdRecord( ) );

        List<RecordField> listRecordFields = RecordFieldHome.getRecordFieldList( recordFieldFilter, pluginDirectory );

        if ( ( listRecordFields != null ) && !listRecordFields.isEmpty( ) )
        {
            for ( RecordField recordField : listRecordFields )
            {
                doRemoveFile( recordField, entry, strWSRestUrl );
            }
        }
    }

    /**
     * Do remove a file from a given record field
     * 
     * @param recordField
     *            the record field
     * @param entry
     *            the entry
     * @param strWSRestUrl
     *            the url of the WS rest
     * @throws BlobStoreClientException
     *             Exception if there is an issue
     */
    public void doRemoveFile( RecordField recordField, IEntry entry, String strWSRestUrl ) throws BlobStoreClientException
    {
        if ( isBlobStoreClientServiceAvailable( ) && ( recordField != null ) )
        {
            // Get the download file url
            String strDownloadFileUrl = entry.convertRecordFieldTitleToString( recordField, null, false );

            if ( StringUtils.isNotBlank( strDownloadFileUrl ) )
            {
                // Parse the download file url to fetch the parameters
                Map<String, List<String>> mapParameters = UrlUtils.getMapParametersFromUrl( strDownloadFileUrl );
                List<String> parameterBlobKey = mapParameters.get( PARAMETER_BLOB_KEY );
                List<String> parameterBlobStore = mapParameters.get( PARAMETER_BLOBSTORE );

                if ( ( parameterBlobKey != null ) && !parameterBlobKey.isEmpty( ) && ( parameterBlobStore != null ) && !parameterBlobStore.isEmpty( ) )
                {
                    String strBlobKey = parameterBlobKey.get( 0 );
                    String strBlobStore = parameterBlobStore.get( 0 );
                    _blobStoreClientService.doDeleteFile( strWSRestUrl, strBlobStore, strBlobKey );
                }
            }
        }
    }

    /**
     * Get the file url
     * 
     * @param strBaseUrl
     *            the base url
     * @param strBlobKey
     *            the blob key
     * @param strBlobStore
     *            the blobstore service name
     * @return the file url
     * @throws BlobStoreClientException
     *             Exception if there is an issue
     */
    public String getFileUrl( String strBaseUrl, String strBlobKey, String strBlobStore ) throws BlobStoreClientException
    {
        if ( isBlobStoreClientServiceAvailable( ) )
        {
            return _blobStoreClientService.getFileUrl( strBaseUrl, strBlobKey, strBlobStore );
        }

        return StringUtils.EMPTY;
    }

    /**
     * Get the file name from a given url
     * 
     * @param strUrl
     *            the url
     * @return the file name
     * @throws BlobStoreClientException
     *             Exception if there is an issue
     */
    public String getFileName( String strUrl ) throws BlobStoreClientException
    {
        if ( isBlobStoreClientServiceAvailable( ) )
        {
            return _blobStoreClientService.getFileName( strUrl );
        }

        return StringUtils.EMPTY;
    }

    /**
     * Do download the file
     * 
     * @param strUrl
     *            the file of the file to download
     * @param strFilePath
     *            the file path to download
     * @throws BlobStoreClientException
     *             exception if there is an error
     */
    public void doDownloadFile( String strUrl, String strFilePath ) throws BlobStoreClientException
    {
        if ( isBlobStoreClientServiceAvailable( ) )
        {
            _blobStoreClientService.doDownloadFile( strUrl, strFilePath );
        }
    }

    /**
     * Do download the file
     * 
     * @param strUrl
     *            the file of the file to download
     * @return a {@link FileItem}
     * @throws BlobStoreClientException
     *             exception if there is an error
     */
    public FileItem doDownloadFile( String strUrl ) throws BlobStoreClientException
    {
        if ( isBlobStoreClientServiceAvailable( ) )
        {
            return _blobStoreClientService.doDownloadFile( strUrl );
        }

        return null;
    }

    /**
     * Gets the fileItem for the entry and the given session.
     * 
     * @param strIdEntry
     *            the entry
     * @param strSessionId
     *            the session id
     * @return the fileItem found, <code>null</code> otherwise.
     */
    public List<FileItem> getFileItems( String strIdEntry, String strSessionId )
    {
        initMap( strSessionId, buildFieldName( strIdEntry ) );

        if ( StringUtils.isBlank( strIdEntry ) )
        {
            throw new AppException( "id entry is not provided for the current file upload" );
        }

        // find session-related files in the map
        Map<String, List<FileItem>> mapFileItemsSession = _mapAsynchronousUpload.get( strSessionId );

        return mapFileItemsSession.get( buildFieldName( strIdEntry ) );
    }

    /**
     * Removes the file from the list.
     *
     * @param strIdEntry
     *            the entry id
     * @param strSessionId
     *            the session id
     * @param nIndex
     *            the n index
     */
    public synchronized void removeFileItem( String strIdEntry, String strSessionId, int nIndex )
    {
        // Remove the file (this will also delete the file physically)
        List<FileItem> uploadedFiles = getFileItems( strIdEntry, strSessionId );

        if ( ( uploadedFiles != null ) && !uploadedFiles.isEmpty( ) && ( uploadedFiles.size( ) > nIndex ) )
        {
            // Remove the object from the Hashmap
            FileItem fileItem = uploadedFiles.remove( nIndex );
            fileItem.delete( );
        }
    }

    /**
     * Removes all files associated to the session
     * 
     * @param strSessionId
     *            the session id
     */
    public synchronized void removeSessionFiles( String strSessionId )
    {
        _mapAsynchronousUpload.remove( strSessionId );
    }

    /**
     * Add file item to the list of uploaded files
     * 
     * @param fileItem
     *            the file item
     * @param strIdEntry
     *            the id entry
     * @param session
     *            the session
     */
    public void addFileItemToUploadedFile( FileItem fileItem, String strIdEntry, HttpSession session )
    {
        // This is the name that will be displayed in the form. We keep
        // the original name, but clean it to make it cross-platform.
        String strFileName = UploadUtil.cleanFileName( FileUploadService.getFileNameOnly( fileItem ) );

        // Check if this file has not already been uploaded
        List<FileItem> uploadedFiles = getFileItems( strIdEntry, session.getId( ) );

        if ( ( uploadedFiles != null ) && !uploadedFiles.isEmpty( ) )
        {
            Iterator<FileItem> iterUploadedFiles = uploadedFiles.iterator( );
            boolean bNew = true;

            while ( bNew && iterUploadedFiles.hasNext( ) )
            {
                FileItem uploadedFile = iterUploadedFiles.next( );
                String strUploadedFileName = UploadUtil.cleanFileName( FileUploadService.getFileNameOnly( uploadedFile ) );
                // If we find a file with the same name and the same
                // length, we consider that the current file has
                // already been uploaded
                bNew = !( strUploadedFileName.equals( strFileName ) && ( uploadedFile.getSize( ) == fileItem.getSize( ) ) );
            }

            if ( !bNew )
            {
                // Delete the temporary file
                // file.delete( );

                // TODO : Raise an error
            }
        }

        if ( uploadedFiles != null )
        {
            uploadedFiles.add( fileItem );
        }
    }

    /**
     * Build the field name from a given id entry i.e. : directory_1
     * 
     * @param strIdEntry
     *            the id entry
     * @return the field name
     */
    public String buildFieldName( String strIdEntry )
    {
        return PREFIX_ENTRY_ID + strIdEntry;
    }

    /**
     * Checks the request parameters to see if an upload submit has been called.
     *
     * @param request
     *            the HTTP request
     * @return the name of the upload action, if any. Null otherwise.
     */
    public String getUploadAction( HttpServletRequest request )
    {
        Enumeration<String> enumParamNames = request.getParameterNames( );

        while ( enumParamNames.hasMoreElements( ) )
        {
            String paramName = enumParamNames.nextElement( );

            if ( paramName.startsWith( UPLOAD_SUBMIT_PREFIX ) || paramName.startsWith( UPLOAD_DELETE_PREFIX ) )
            {
                return paramName;
            }
        }

        return null;
    }

    /**
     * Performs an upload action.
     *
     * @param request
     *            the HTTP request
     * @param strUploadAction
     *            the name of the upload action
     * @param map
     *            the map of idEntry, RecordFields 
     * @param record
     *            the record
     * @param plugin
     *            the plugin
     * @throws DirectoryErrorException
     *             exception if there is an error
     */
    public void doUploadAction( HttpServletRequest request, String strUploadAction, Map<String, List<RecordField>> map, Record record, Plugin plugin )
            throws DirectoryErrorException
    {
        String strIdEntry = findIdEntryFromAction( strUploadAction );

        if ( strUploadAction.startsWith( UPLOAD_SUBMIT_PREFIX ) )
        {
            doUploadFile( request, strIdEntry, map, record );
        }
        else
            if ( strUploadAction.startsWith( UPLOAD_DELETE_PREFIX ) )
            {
                doDeleteFile( request, strIdEntry, map );
            }
    }

    /**
     * Finds the entry id from the specified action
     * 
     * @param strAction
     *            the action
     * @return the entry id as a {@code String}
     */
    private String findIdEntryFromAction( String strAction )
    {
        return strAction.startsWith( UPLOAD_SUBMIT_PREFIX ) ? strAction.substring( UPLOAD_SUBMIT_PREFIX.length( ) ) : strAction.substring( UPLOAD_DELETE_PREFIX
                .length( ) );
    }

    /**
     * Uploads a file
     * 
     * @param request
     *            the request containing the file to upload
     * @param strIdEntry
     *            the entry id linked to the file
     * @param map
     *            the map of {@code RecordFields}
     * @param record
     *            the record on which the file is added
     * @throws DirectoryErrorException
     *             if there is an error during the upload
     */
    public void doUploadFile( HttpServletRequest request, String strIdEntry, Map<String, List<RecordField>> map, Record record ) throws DirectoryErrorException
    {
        FileItem fileUploaded = findUploadedFileFromRequest( request, strIdEntry );

        if ( fileUploaded != null && StringUtils.isNotBlank( fileUploaded.getName( ) ) )
        {
            canUploadFile( request, fileUploaded, strIdEntry );

            List<RecordField> listRecordFields = findRecordFields( map, strIdEntry );
            RecordField recordFieldForUploadedFile = createRecordFieldForUploadedFile( fileUploaded, strIdEntry );
            addRecordFieldForUploadedFile( recordFieldForUploadedFile, record, listRecordFields );
            addFileItemToUploadedFile( fileUploaded, strIdEntry, request.getSession( ) );
        }
    }

    /**
     * Finds the uploaded file from the specified request
     * 
     * @param request
     *            the request containing the file to upload
     * @param strIdEntry
     *            the entry id linked to the file
     * @return the uploaded file as a {@code FileItem}
     */
    private FileItem findUploadedFileFromRequest( HttpServletRequest request, String strIdEntry )
    {
        MultipartHttpServletRequest multipartRequest = (MultipartHttpServletRequest) request;
        String strFileParameter = buildFieldName( strIdEntry );

        return multipartRequest.getFile( strFileParameter );
    }

    /**
     * Tests if the file can be uploaded
     * 
     * @param request
     *            the request used to test
     * @param fileItem
     *            the file to test
     * @param strIdEntry
     *            the entry id linked to the file
     * @throws DirectoryErrorException
     *             if the file cannot be uploaded
     */
    private void canUploadFile( HttpServletRequest request, FileItem fileItem, String strIdEntry ) throws DirectoryErrorException
    {
        HttpSession session = request.getSession( );
        String strFieldName = buildFieldName( strIdEntry );
        List<FileItem> listFileItemsToUpload = new ArrayList<FileItem>( );
        listFileItemsToUpload.add( fileItem );

        canUploadFiles( strFieldName, getFileItems( strIdEntry, session.getId( ) ), listFileItemsToUpload, request.getLocale( ) );
    }

    /**
     * Finds the record fields linked to the specified entry from the specified map
     * 
     * @param map
     *            the map of {@code RecordFields}
     * @param strIdEntry
     *            the entry id
     * @return the list of record fields
     */
    private List<RecordField> findRecordFields( Map<String, List<RecordField>> map, String strIdEntry )
    {
        List<RecordField> listRecordFields = map.get( strIdEntry );

        if ( listRecordFields == null )
        {
            listRecordFields = new ArrayList<RecordField>( );
            map.put( strIdEntry, listRecordFields );
        }

        return listRecordFields;
    }

    /**
     * Creates a {@code RecordField} for the uploaded file
     * 
     * @param fileItem
     *            the uploaded file
     * @param strIdEntry
     *            the entry id linked to the file
     * @return the created {@code RecordField}
     */
    private RecordField createRecordFieldForUploadedFile( FileItem fileItem, String strIdEntry )
    {
        RecordField recordFieldForUploadedFile = null;
        Plugin plugin = PluginService.getPlugin( DirectoryPlugin.PLUGIN_NAME );

        // Add the file to the map of idEntry, RecordFields
        IEntry entry = EntryHome.findByPrimaryKey( DirectoryUtils.convertStringToInt( strIdEntry ), plugin );

        if ( entry != null )
        {
            recordFieldForUploadedFile = new RecordField( );
            recordFieldForUploadedFile.setEntry( entry );

            String strFilename = FileUploadService.getFileNameOnly( fileItem );

            if ( ( fileItem.get( ) != null ) && ( fileItem.getSize( ) < Integer.MAX_VALUE ) )
            {
                if ( entry instanceof EntryTypeDownloadUrl )
                {
                    recordFieldForUploadedFile.setFileName( strFilename );
                    recordFieldForUploadedFile.setFileExtension( FileSystemUtil.getMIMEType( strFilename ) );
                }
                else
                {
                    PhysicalFile physicalFile = new PhysicalFile( );
                    physicalFile.setValue( fileItem.get( ) );

                    File file = new File( );
                    file.setPhysicalFile( physicalFile );
                    file.setTitle( strFilename );
                    file.setSize( (int) fileItem.getSize( ) );
                    file.setMimeType( FileSystemUtil.getMIMEType( strFilename ) );

                    recordFieldForUploadedFile.setFile( file );
                }
            }
        }

        return recordFieldForUploadedFile;
    }

    /**
     * Adds the record field in the specified record and in the specified list
     * 
     * @param recordFieldForUploadedFile
     *            the record field to add
     * @param record
     *            the record
     * @param listRecordFields
     *            the list of record fields
     */
    private void addRecordFieldForUploadedFile( RecordField recordFieldForUploadedFile, Record record, List<RecordField> listRecordFields )
    {
        if ( recordFieldForUploadedFile != null )
        {
            recordFieldForUploadedFile.setRecord( record );
            listRecordFields.add( recordFieldForUploadedFile );
        }
    }

    /**
     * Deletes a file
     * 
     * @param request
     *            the request containing the file to delete
     * @param strIdEntry
     *            the entry id linked to the file
     * @param map
     *            the map of {@code RecordFields}
     */
    public void doDeleteFile( HttpServletRequest request, String strIdEntry, Map<String, List<RecordField>> map )
    {
        HttpSession session = request.getSession( false );

        if ( session != null )
        {
            // Some previously uploaded files were deleted
            // Build the prefix of the associated checkboxes
            String strPrefix = UPLOAD_CHECKBOX_PREFIX + strIdEntry;

            // Look for the checkboxes in the request
            Enumeration<String> enumParamNames = request.getParameterNames( );
            List<Integer> listIndexes = new ArrayList<Integer>( );

            while ( enumParamNames.hasMoreElements( ) )
            {
                String strParamName = enumParamNames.nextElement( );

                if ( strParamName.startsWith( strPrefix ) )
                {
                    // Get the index from the name of the checkbox
                    listIndexes.add( Integer.parseInt( strParamName.substring( strPrefix.length( ) ) ) );
                }
            }

            Collections.sort( listIndexes );
            Collections.reverse( listIndexes );

            for ( int nIndex : listIndexes )
            {
                // Remove from the map of <idEntry, RecordField>
                List<RecordField> listRecordFields = map.get( strIdEntry );

                if ( listRecordFields != null )
                {
                    listRecordFields.remove( nIndex );
                }

                // Remove from the asynchronous uploaded files map
                removeFileItem( strIdEntry, session.getId( ), nIndex );
            }
        }
    }

    /**
     * Performs an upload action.
     *
     * @param request
     *            the HTTP request
     * @param strUploadAction
     *            the name of the upload action
     * @param map
     *            the map of idEntry, RecordFields
     * @throws DirectoryErrorException
     *             exception if there is an error
     */
    public void doUploadAction( HttpServletRequest request, String strUploadAction, Map<String, List<RecordField>> map ) throws DirectoryErrorException
    {
        String strIdEntry = findIdEntryFromAction( strUploadAction );

        if ( strUploadAction.startsWith( UPLOAD_SUBMIT_PREFIX ) )
        {
            doUploadFile( request, strIdEntry, map );
        }
        else
            if ( strUploadAction.startsWith( UPLOAD_DELETE_PREFIX ) )
            {
                doDeleteFile( request, strIdEntry, map );
            }
    }

    /**
     * Uploads a file
     * 
     * @param request
     *            the request containing the file to upload
     * @param strIdEntry
     *            the entry id linked to the file
     * @param map
     *            the map of {@code RecordFields}
     * @throws DirectoryErrorException
     *             if there is an error during the upload
     */
    public void doUploadFile( HttpServletRequest request, String strIdEntry, Map<String, List<RecordField>> map ) throws DirectoryErrorException
    {
        FileItem fileUploaded = findUploadedFileFromRequest( request, strIdEntry );

        if ( fileUploaded != null && StringUtils.isNotBlank( fileUploaded.getName( ) ) )
        {
            canUploadFile( request, fileUploaded, strIdEntry );

            addFileItemToUploadedFile( fileUploaded, strIdEntry, request.getSession( ) );
        }
    }

    /**
     * Reinit the map with the default files stored in database and blobstore
     * 
     * @param request
     *            the HTTP request
     * @param map
     *            the map idEntry, RecordFields
     * @param plugin
     *            the plugin
     */
    public void reinitMap( HttpServletRequest request, Map<String, List<RecordField>> map, Plugin plugin )
    {
        HttpSession session = request.getSession( );
        removeSessionFiles( session.getId( ) );

        if ( ( map != null ) && !map.isEmpty( ) )
        {
            for ( java.util.Map.Entry<String, List<RecordField>> param : map.entrySet( ) )
            {
                for ( RecordField recordField : param.getValue( ) )
                {
                    if ( recordField != null )
                    {
                        IEntry entry = recordField.getEntry( );

                        if ( ( recordField.getFile( ) != null ) && ( recordField.getFile( ).getPhysicalFile( ) != null ) && !recordField.isLittleThumbnail( )
                                && !recordField.isBigThumbnail( ) )
                        {
                            // The little thumbnail and the big thumbnail should not be stored in the session
                            File file = recordField.getFile( );
                            PhysicalFile physicalFile = PhysicalFileHome.findByPrimaryKey( file.getPhysicalFile( ).getIdPhysicalFile( ), plugin );
                            FileItem fileItem = new DirectoryFileItem( physicalFile.getValue( ), file.getTitle( ) );
                            // Add the file item to the map
                            addFileItemToUploadedFile( fileItem, Integer.toString( entry.getIdEntry( ) ), session );
                        }
                        else
                            if ( recordField.getEntry( ) instanceof EntryTypeDownloadUrl && isBlobStoreClientServiceAvailable( ) )
                            {
                                // Different behaviour if the entry is an EntryTypeDownloadUrl
                                FileItem fileItem;

                                try
                                {
                                    fileItem = doDownloadFile( recordField.getValue( ) );

                                    FileItem directoryFileItem = new DirectoryFileItem( fileItem.get( ), fileItem.getName( ) );
                                    // Add the file item to the map
                                    addFileItemToUploadedFile( directoryFileItem, Integer.toString( entry.getIdEntry( ) ), session );
                                }
                                catch( BlobStoreClientException e )
                                {
                                    AppLogService.error( DirectoryAsynchronousUploadHandler.class.getName( ) + " - Error when reinit map. Cause : "
                                            + e.getMessage( ) );
                                }
                            }
                    }
                }
            }
        }
    }

    /**
     * Init the map
     * 
     * @param strSessionId
     *            the session id
     * @param strFieldName
     *            the field name
     */
    private void initMap( String strSessionId, String strFieldName )
    {
        // find session-related files in the map
        Map<String, List<FileItem>> mapFileItemsSession = _mapAsynchronousUpload.get( strSessionId );

        // create map if not exists
        if ( mapFileItemsSession == null )
        {
            synchronized( _mapAsynchronousUpload )
            {
                if ( _mapAsynchronousUpload.get( strSessionId ) == null )
                {
                    mapFileItemsSession = new ConcurrentHashMap<String, List<FileItem>>( );
                    _mapAsynchronousUpload.put( strSessionId, mapFileItemsSession );
                }
            }
        }

        if ( mapFileItemsSession != null )
        {
            List<FileItem> listFileItems = mapFileItemsSession.get( strFieldName );

            if ( listFileItems == null )
            {
                listFileItems = new ArrayList<FileItem>( );
                mapFileItemsSession.put( strFieldName, listFileItems );
            }
        }
    }

    /**
     * Check if the file can be uploaded or not. This method will check the size of each file and the number max of files that can be uploaded.
     * 
     * @param strFieldName
     *            the field name
     * @param listUploadedFileItems
     *            the list of uploaded files
     * @param listFileItemsToUpload
     *            the list of files to upload
     * @param mainObject
     *            the JSON object to complete if there is an error
     * @param locale
     *            the locale
     * @return true if the list of files can be uploaded, false otherwise
     * category CALLED_BY_JS (directoryupload.js)
     */
    private boolean canUploadFiles( String strFieldName, List<FileItem> listUploadedFileItems, List<FileItem> listFileItemsToUpload, JSONObject mainObject,
            Locale locale )
    {
        if ( StringUtils.isNotBlank( strFieldName ) )
        {
            String strIdEntry = strFieldName.substring( PREFIX_ENTRY_ID.length( ) );
            int nIdEntry = DirectoryUtils.convertStringToInt( strIdEntry );
            IEntry entry = EntryHome.findByPrimaryKey( nIdEntry, DirectoryUtils.getPlugin( ) );

            if ( entry != null )
            {
                try
                {
                    entry.canUploadFiles( listUploadedFileItems, listFileItemsToUpload, locale );
                }
                catch( DirectoryErrorException e )
                {
                    JSONUtils.buildJsonError( mainObject, e.getErrorMessage( ) );

                    return false;
                }

                return true;
            }
        }

        return false;
    }

    /**
     * Check if the file can be uploaded or not. This method will check the size of each file and the number max of files that can be uploaded.
     * 
     * @param strFieldName
     *            the field name
     * @param listUploadedFileItems
     *            the list of uploaded files
     * @param listFileItemsToUpload
     *            the list of files to upload
     * @param locale
     *            the locale
     * @throws DirectoryErrorException
     *             exception if there is an error
     */
    private void canUploadFiles( String strFieldName, List<FileItem> listUploadedFileItems, List<FileItem> listFileItemsToUpload, Locale locale )
            throws DirectoryErrorException
    {
        if ( StringUtils.isNotBlank( strFieldName ) )
        {
            String strIdEntry = strFieldName.substring( PREFIX_ENTRY_ID.length( ) );
            int nIdEntry = DirectoryUtils.convertStringToInt( strIdEntry );
            IEntry entry = EntryHome.findByPrimaryKey( nIdEntry, DirectoryUtils.getPlugin( ) );

            if ( entry != null )
            {
                entry.canUploadFiles( listUploadedFileItems, listFileItemsToUpload, locale );
            }
        }
    }

    /**
     *
     * DirectoryFileItem : builds fileItem from json response.
     *
     */
    private static class DirectoryFileItem implements FileItem
    {
        private static final long serialVersionUID = 1L;
        private byte [ ] _bValue;
        private final String _strFileName;
        private FileItemHeaders _fileItemHeaders;

        /**
         * FormFileItem
         * 
         * @param bValue
         *            the byte value
         * @param strFileName
         *            the file name
         */
        public DirectoryFileItem( byte [ ] bValue, String strFileName )
        {
            _bValue = bValue;
            _strFileName = strFileName;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void delete( )
        {
            _bValue = null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public byte [ ] get( )
        {
            return _bValue;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getContentType( )
        {
            return FileSystemUtil.getMIMEType( _strFileName );
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getFieldName( )
        {
            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public InputStream getInputStream( ) throws IOException
        {
            return new ByteArrayInputStream( _bValue );
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getName( )
        {
            return _strFileName;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public OutputStream getOutputStream( ) throws IOException
        {
            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public long getSize( )
        {
            return _bValue.length;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getString( )
        {
            return new String( _bValue );
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getString( String encoding ) throws UnsupportedEncodingException
        {
            return new String( _bValue, encoding );
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isFormField( )
        {
            return false;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isInMemory( )
        {
            return true;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void setFieldName( String strName )
        {
            // nothing
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void setFormField( boolean bState )
        {
            // nothing
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void write( java.io.File file ) throws Exception
        {
            // nothing
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public FileItemHeaders getHeaders( )
        {
            return _fileItemHeaders;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void setHeaders( FileItemHeaders headers )
        {
            _fileItemHeaders = headers;
        }
    }
}
