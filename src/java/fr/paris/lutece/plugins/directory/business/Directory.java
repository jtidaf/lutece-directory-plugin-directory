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

import fr.paris.lutece.plugins.directory.business.attribute.DirectoryAttribute;
import fr.paris.lutece.plugins.directory.business.rss.DirectoryResourceRssConfigRemovalListener;
import fr.paris.lutece.plugins.directory.service.DirectoryXslRemovalListenerService;
import fr.paris.lutece.plugins.directory.service.FileImgService;
import fr.paris.lutece.plugins.directory.utils.DirectoryUtils;
import fr.paris.lutece.portal.service.plugin.Plugin;
import fr.paris.lutece.portal.service.rbac.RBACResource;
import fr.paris.lutece.portal.service.regularexpression.RegularExpressionRemovalListenerService;
import fr.paris.lutece.portal.service.workflow.WorkflowRemovalListenerService;
import fr.paris.lutece.portal.service.workgroup.AdminWorkgroupResource;
import fr.paris.lutece.portal.service.workgroup.WorkgroupRemovalListenerService;
import fr.paris.lutece.util.date.DateUtil;
import fr.paris.lutece.util.xml.XmlUtil;

import java.sql.Timestamp;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;

/**
 *
 * Class Directory
 *
 */
public class Directory implements AdminWorkgroupResource, RBACResource
{
    public static final String ROLE_NONE = "none";
    public static final String RESOURCE_TYPE = "DIRECTORY_DIRECTORY_TYPE";
    public static final int STATE_ENABLE = 1;
    public static final int STATE_DISABLE = 0;
    private static final String TAG_LIST_ENTRY = "list-entry";
    private static final String TAG_DIRECTORY = "directory";
    private static final String TAG_TITLE = "title";
    private static final String TAG_LIST_RECORD = "list-record";
    private static final String TAG_CREATION_DATE = "creation-date";
    private static final String ATTRIBUTE_ID = "id";
    private static DirectoryWorkgroupRemovalListener _listenerWorkgroup;
    private static DirectoryRegularExpressionRemovalListener _listenerRegularExpression;
    private static DirectoryXslRemovalListener _listenerXslRemovalListener;
    private static DirectoryWorkflowRemovalListener _listenerWorkflowRemovalListener;
    private static EntryTypeDirectoryRemovalListener _listenerEntryDirectoryRemovalListener;
    private static DirectoryResourceRssConfigRemovalListener _listenerDirectoryResourceRssConfigRemovalListener;
    private int _nIdDirectory;
    private String _strTitle;
    private String _strFrontOfficeTitle;
    private String _strDescription;
    private String _strUnavailabilityMessage;
    private String _strWorkgroupKey;
    private String _strRoleKey;
    private Timestamp _tDateCreation;
    private boolean _bIsEnabled;
    private int _nIdResultListTemplate = DirectoryUtils.CONSTANT_ID_NULL;
    private int _nIdResultRecordTemplate = DirectoryUtils.CONSTANT_ID_NULL;
    private int _nIdFormSearchTemplate = DirectoryUtils.CONSTANT_ID_NULL;
    private int _nNumberRecordPerPage;
    private List<DirectoryAction> _listActions;
    private int _nIdWorkflow = DirectoryUtils.CONSTANT_ID_NULL;
    private int _nIdWorkflowStateToRemove = DirectoryUtils.CONSTANT_ID_NULL;
    private boolean _bDisplaySearchState;
    private boolean _bDisplaySearchComplementaryState;
    private String _strSortEntryId;
    private boolean _bAscSort;
    private String _strSortEntryIdFront;
    private boolean _bAscSortFront;
    private boolean _bRecordActivated;
    private boolean _bIsIndexed;
    @DirectoryAttribute( "searchOperatorOr" )
    private boolean _bSearchOperatorOr;

    // Creation date field
    @DirectoryAttribute( "dateShownInResultList" )
    private boolean _bDateShownInResultList;
    @DirectoryAttribute( "dateShownInResultRecord" )
    private boolean _bDateShownInResultRecord;
    @DirectoryAttribute( "dateShownInHistory" )
    private boolean _bDateShownInHistory;
    @DirectoryAttribute( "dateShownInSearch" )
    private boolean _bDateShownInSearch;
    @DirectoryAttribute( "dateShownInAdvancedSearch" )
    private boolean _bDateShownInAdvancedSearch;
    @DirectoryAttribute( "dateShownInMultiSearch" )
    private boolean _bDateShownInMultiSearch;
    @DirectoryAttribute( "dateShownInExport" )
    private boolean _bDateShownInExport;

    // Modification date field
    @DirectoryAttribute( "dateModificationShownInResultList" )
    private boolean _bDateModificationShownInResultList;
    @DirectoryAttribute( "dateModificationShownInResultRecord" )
    private boolean _bDateModificationShownInResultRecord;
    @DirectoryAttribute( "dateModificationShownInHistory" )
    private boolean _bDateModificationShownInHistory;
    @DirectoryAttribute( "dateModificationShownInSearch" )
    private boolean _bDateModificationShownInSearch;
    @DirectoryAttribute( "dateModificationShownInAdvancedSearch" )
    private boolean _bDateModificationShownInAdvancedSearch;
    @DirectoryAttribute( "dateModificationShownInMultiSearch" )
    private boolean _bDateModificationShownInMultiSearch;
    @DirectoryAttribute( "dateModificationShownInExport" )
    private boolean _bDateModificationShownInExport;

    /**
     * Initialize the Directory
     */
    public static void init( )
    {
        // Create removal listeners and register them
        if ( _listenerWorkgroup == null )
        {
            _listenerWorkgroup = new DirectoryWorkgroupRemovalListener( );
            WorkgroupRemovalListenerService.getService( ).registerListener( _listenerWorkgroup );
        }

        if ( _listenerRegularExpression == null )
        {
            _listenerRegularExpression = new DirectoryRegularExpressionRemovalListener( );
            RegularExpressionRemovalListenerService.getService( ).registerListener( _listenerRegularExpression );
        }

        if ( _listenerXslRemovalListener == null )
        {
            _listenerXslRemovalListener = new DirectoryXslRemovalListener( );
            DirectoryXslRemovalListenerService.getService( ).registerListener( _listenerXslRemovalListener );
        }

        if ( _listenerWorkflowRemovalListener == null )
        {
            _listenerWorkflowRemovalListener = new DirectoryWorkflowRemovalListener( );
            WorkflowRemovalListenerService.getService( ).registerListener( _listenerWorkflowRemovalListener );
        }

        if ( _listenerEntryDirectoryRemovalListener == null )
        {
            _listenerEntryDirectoryRemovalListener = new EntryTypeDirectoryRemovalListener( );
            EntryRemovalListenerService.getService( ).registerListener( _listenerEntryDirectoryRemovalListener );
        }

        if ( _listenerDirectoryResourceRssConfigRemovalListener == null )
        {
            _listenerDirectoryResourceRssConfigRemovalListener = new DirectoryResourceRssConfigRemovalListener( );
            EntryRemovalListenerService.getService( ).registerListener( _listenerDirectoryResourceRssConfigRemovalListener );
        }

        // ImageResourceManager
        FileImgService.getInstance( ).register( );
    }

    /**
     *
     * @return the title of the directory
     */
    public String getTitle( )
    {
        return _strTitle;
    }

    /**
     * set the title of the directory
     * 
     * @param strTitle
     *            the title of the directory
     */
    public void setTitle( String strTitle )
    {
        _strTitle = strTitle;
    }

    /**
     *
     * @return the front office title of the directory
     */
    public String getFrontOfficeTitle( )
    {
        return _strFrontOfficeTitle;
    }

    /**
     * set the front office title of the directory
     * 
     * @param strFrontOfficeTitle
     *            the front office title of the directory
     */
    public void setFrontOfficeTitle( String strFrontOfficeTitle )
    {
        _strFrontOfficeTitle = strFrontOfficeTitle;
    }

    /**
     *
     * @return the description of the directory
     */
    public String getDescription( )
    {
        return _strDescription;
    }

    /**
     * @return the _bDisplaySearchState
     */
    public boolean isDisplaySearchState( )
    {
        return _bDisplaySearchState;
    }

    /**
     * @param displayState
     *            the _bDisplaySearchState to set
     */
    public void setDisplaySearchState( boolean displayState )
    {
        _bDisplaySearchState = displayState;
    }

    /**
     * @return the _bDisplayComplementarySearchState
     */
    public boolean isDisplayComplementarySearchState( )
    {
        return _bDisplaySearchComplementaryState;
    }

    /**
     * @param displayState
     *            the _bDisplayComplementarySearchState to set
     */
    public void setDisplayComplementarySearchState( boolean displayState )
    {
        _bDisplaySearchComplementaryState = displayState;
    }

    /**
     * set the description of the directory
     * 
     * @param description
     *            the description of the directory
     */
    public void setDescription( String description )
    {
        this._strDescription = description;
    }

    /**
     *
     * @return the unavailability message of the directory
     */
    public String getUnavailabilityMessage( )
    {
        return _strUnavailabilityMessage;
    }

    /**
     * set the unavailability message of the directory
     * 
     * @param unavailabilityMessage
     *            the unavailability message of the directory
     */
    public void setUnavailabilityMessage( String unavailabilityMessage )
    {
        _strUnavailabilityMessage = unavailabilityMessage;
    }

    /**
     * Get the workgroup key of the directory
     * 
     * @return the work group associate to the directory
     */
    @Override
    public String getWorkgroup( )
    {
        return _strWorkgroupKey;
    }

    /**
     * set the work group associate to the directory
     * 
     * @param workGroup
     *            the work group associate to the directory
     */
    public void setWorkgroup( String workGroup )
    {
        _strWorkgroupKey = workGroup;
    }

    /**
     *
     * @return the id of the directory
     */
    public int getIdDirectory( )
    {
        return _nIdDirectory;
    }

    /**
     * set the id of the directory
     * 
     * @param idDirectory
     *            the id of the directory
     */
    public void setIdDirectory( int idDirectory )
    {
        _nIdDirectory = idDirectory;
    }

    /**
     *
     * @return true if the directory is enabled
     */
    public boolean isEnabled( )
    {
        return _bIsEnabled;
    }

    /**
     * set true if the directory is enabled
     * 
     * @param enable
     *            true if the directory is enabled
     */
    public void setEnabled( boolean enable )
    {
        _bIsEnabled = enable;
    }

    /**
     *
     * @return the creation date
     */
    public Timestamp getDateCreation( )
    {
        return _tDateCreation;
    }

    /**
     * set the creation date
     * 
     * @param dateCreation
     *            the creation date
     */
    public void setDateCreation( Timestamp dateCreation )
    {
        _tDateCreation = dateCreation;
    }

    /**
     * RBAC resource implementation
     * 
     * @return The resource type code
     */
    @Override
    public String getResourceTypeCode( )
    {
        return RESOURCE_TYPE;
    }

    /**
     * RBAC resource implementation
     * 
     * @return The resourceId
     */
    @Override
    public String getResourceId( )
    {
        return "" + _nIdDirectory;
    }

    /**
     *
     * @return a list of action can be use for the directory
     */
    public List<DirectoryAction> getActions( )
    {
        return _listActions;
    }

    /**
     * set a list of action can be use for the directory
     * 
     * @param directoryActions
     *            a list of action must be use for the directory
     */
    public void setActions( List<DirectoryAction> directoryActions )
    {
        _listActions = directoryActions;
    }

    /**
     * Gets the directory role
     * 
     * @return directory's role as a String
     *
     */
    public String getRoleKey( )
    {
        return _strRoleKey;
    }

    /**
     * Sets the directory's role
     * 
     * @param strRole
     *            The role
     *
     */
    public void setRoleKey( String strRole )
    {
        _strRoleKey = strRole;
    }

    /**
     * return the id of the result list template
     * 
     * @return the id of the result list template
     */
    public int getIdResultListTemplate( )
    {
        return _nIdResultListTemplate;
    }

    /**
     * set the id of the result list template
     * 
     * @param idResultListTemplate
     *            the id of the result list template
     */
    public void setIdResultListTemplate( int idResultListTemplate )
    {
        _nIdResultListTemplate = idResultListTemplate;
    }

    /**
     * return the id of the record template
     * 
     * @return he id of the record template
     */
    public int getIdResultRecordTemplate( )
    {
        return _nIdResultRecordTemplate;
    }

    /**
     * set the id of the record template
     * 
     * @param idResultRecordTemplate
     *            the id of the record template
     */
    public void setIdResultRecordTemplate( int idResultRecordTemplate )
    {
        _nIdResultRecordTemplate = idResultRecordTemplate;
    }

    /**
     * return the id of the form search template
     * 
     * @return the id of the form search template
     */
    public int getIdFormSearchTemplate( )
    {
        return _nIdFormSearchTemplate;
    }

    /**
     *
     * @return the number of record display per page
     */
    public int getNumberRecordPerPage( )
    {
        return _nNumberRecordPerPage;
    }

    /**
     * the number of record display per page
     * 
     * @param numberRecordPerPage
     *            the number of record display per page
     */
    public void setNumberRecordPerPage( int numberRecordPerPage )
    {
        _nNumberRecordPerPage = numberRecordPerPage;
    }

    /**
     * set the id of the form search template
     * 
     * @param idFormSearchTemplate
     *            the id of the form search template
     */
    public void setIdFormSearchTemplate( int idFormSearchTemplate )
    {
        _nIdFormSearchTemplate = idFormSearchTemplate;
    }

    /**
     *
     * @return the id of the directory workflow
     */
    public int getIdWorkflow( )
    {
        return _nIdWorkflow;
    }

    /**
     * set the id of the directory worflow
     * 
     * @param nIdWorkflow
     *            the id of the directory worflow
     */
    public void setIdWorkflow( int nIdWorkflow )
    {
        _nIdWorkflow = nIdWorkflow;
    }

    /**
     * Get the id of the workflow state of records that must be permanently removed
     * 
     * @return The id of the workflow state of records that must be permanently removed
     */
    public int getIdWorkflowStateToRemove( )
    {
        return _nIdWorkflowStateToRemove;
    }

    /**
     * Set the id of the workflow state of records that must be permanently removed
     * 
     * @param nIdWorkflowStateToRemove
     *            The id of the workflow state of records that must be permanently removed
     */
    public void setIdWorkflowStateToRemove( int nIdWorkflowStateToRemove )
    {
        this._nIdWorkflowStateToRemove = nIdWorkflowStateToRemove;
    }

    /**
     * Check if the creation date should be shown in the result list
     * 
     * @return True if the creation date should be shown in the result list, false otherwise
     */
    public boolean isDateShownInResultList( )
    {
        return _bDateShownInResultList;
    }

    /**
     * Set whether the creation date should be shown in the result list
     * 
     * @param bDateShownInResultList
     *            True if the creation date should be shown in the result list, false otherwise
     */
    public void setDateShownInResultList( boolean bDateShownInResultList )
    {
        _bDateShownInResultList = bDateShownInResultList;
    }

    /**
     * Check if the creation date should be shown in the result record
     * 
     * @return True if the creation date should be shown in the result record, false otherwise
     */
    public boolean isDateShownInResultRecord( )
    {
        return _bDateShownInResultRecord;
    }

    /**
     * Set whether the creation date should be shown in the result record
     * 
     * @param bDateShownInResultRecord
     *            True if the creation date should be shown in the result record, false otherwise
     */
    public void setDateShownInResultRecord( boolean bDateShownInResultRecord )
    {
        _bDateShownInResultRecord = bDateShownInResultRecord;
    }

    /**
     * Check if the creation date should be shown in history
     * 
     * @return True if the creation date should be shown in history, false otherwise
     */
    public boolean isDateShownInHistory( )
    {
        return _bDateShownInHistory;
    }

    /**
     * Set whether the creation date should be shown in the history
     * 
     * @param bDateShownInHistory
     *            True if the creation date should be shown in the history, false otherwise
     */
    public void setDateShownInHistory( boolean bDateShownInHistory )
    {
        _bDateShownInHistory = bDateShownInHistory;
    }

    /**
     * Check if the creation date should be shown in search
     * 
     * @return True if the creation date should be shown in search, false otherwise
     */
    public boolean isDateShownInSearch( )
    {
        return _bDateShownInSearch;
    }

    /**
     * Set whether the creation date should be shown in the search
     * 
     * @param bDateShownInSearch
     *            True if the creation date should be shown in the search, false otherwise
     */
    public void setDateShownInSearch( boolean bDateShownInSearch )
    {
        _bDateShownInSearch = bDateShownInSearch;
    }

    /**
     * Check if the creation date should be shown in advanced search
     * 
     * @return True if the creation date should be shown in advanced search, false otherwise
     */
    public boolean isDateShownInAdvancedSearch( )
    {
        return _bDateShownInAdvancedSearch;
    }

    /**
     * Set whether the creation date should be shown in the advanced search
     * 
     * @param bDateShownInAdvancedSearch
     *            True if the creation date should be shown in the advanced search, false otherwise
     */
    public void setDateShownInAdvancedSearch( boolean bDateShownInAdvancedSearch )
    {
        _bDateShownInAdvancedSearch = bDateShownInAdvancedSearch;
    }

    /**
     * Check if the creation date should be shown in multi search
     * 
     * @return True if the creation date should be shown in multi search, false otherwise
     */
    public boolean isDateShownInMultiSearch( )
    {
        return _bDateShownInMultiSearch;
    }

    /**
     * Set whether the creation date should be shown in the multi search
     * 
     * @param bDateShownInMultiSearch
     *            True if the creation date should be shown in the multi search, false otherwise
     */
    public void setDateShownInMultiSearch( boolean bDateShownInMultiSearch )
    {
        _bDateShownInMultiSearch = bDateShownInMultiSearch;
    }

    /**
     * Check if the creation date should be exported
     * 
     * @return True if the creation date should be exported, false otherwise
     */
    public boolean isDateShownInExport( )
    {
        return _bDateShownInExport;
    }

    /**
     * Set whether the creation date should be exported
     * 
     * @param bDateShownInExport
     *            True if the creation date should be exported, false otherwise
     */
    public void setDateShownInExport( boolean bDateShownInExport )
    {
        _bDateShownInExport = bDateShownInExport;
    }

    /**
     * Check if the modification date should be shown in the result list
     * 
     * @return True if the modification date should be shown in the result list, false otherwise
     */
    public boolean isDateModificationShownInResultList( )
    {
        return _bDateModificationShownInResultList;
    }

    /**
     * Set whether the modification date should be shown in the result list
     * 
     * @param bDateModificationShownInResultList
     *            True if the modification date should be shown in the result list, false otherwise
     */
    public void setDateModificationShownInResultList( boolean bDateModificationShownInResultList )
    {
        _bDateModificationShownInResultList = bDateModificationShownInResultList;
    }

    /**
     * Check if the modification date should be shown in the result record
     * 
     * @return True if the modification date should be shown in the result record, false otherwise
     */
    public boolean isDateModificationShownInResultRecord( )
    {
        return _bDateModificationShownInResultRecord;
    }

    /**
     * Set whether the modification date should be shown in the result record
     * 
     * @param bDateModificationShownInResultRecord
     *            True if the modification date should be shown in the result record, false otherwise
     */
    public void setDateModificationShownInResultRecord( boolean bDateModificationShownInResultRecord )
    {
        _bDateModificationShownInResultRecord = bDateModificationShownInResultRecord;
    }

    /**
     * Check if the modification date should be shown in history
     * 
     * @return True if the modification date should be shown in history, false otherwise
     */
    public boolean isDateModificationShownInHistory( )
    {
        return _bDateModificationShownInHistory;
    }

    /**
     * Set whether the modification date should be shown in history
     * 
     * @param bDateModificationShownInHistory
     *            True if the modification date should be shown in history, false otherwise
     */
    public void setDateModificationShownInHistory( boolean bDateModificationShownInHistory )
    {
        _bDateModificationShownInHistory = bDateModificationShownInHistory;
    }

    /**
     * Check if the modification date should be shown in search
     * 
     * @return True if the modification date should be shown in search, false otherwise
     */
    public boolean isDateModificationShownInSearch( )
    {
        return _bDateModificationShownInSearch;
    }

    /**
     * Set whether the modification date should be shown in search
     * 
     * @param bDateModificationShownInSearch
     *            True if the modification date should be shown in search, false otherwise
     */
    public void setDateModificationShownInSearch( boolean bDateModificationShownInSearch )
    {
        _bDateModificationShownInSearch = bDateModificationShownInSearch;
    }

    /**
     * Check if the modification date should be shown in advanced search
     * 
     * @return True if the modification date should be shown in advanced search, false otherwise
     */
    public boolean isDateModificationShownInAdvancedSearch( )
    {
        return _bDateModificationShownInAdvancedSearch;
    }

    /**
     * Set whether the modification date should be shown in advanced search
     * 
     * @param bDateModificationShownInAdvancedSearch
     *            True if the modification date should be shown in advanced search, false otherwise
     */
    public void setDateModificationShownInAdvancedSearch( boolean bDateModificationShownInAdvancedSearch )
    {
        _bDateModificationShownInAdvancedSearch = bDateModificationShownInAdvancedSearch;
    }

    /**
     * Check if the modification date should be shown in multi search
     * 
     * @return True if the modification date should be shown in multi search, false otherwise
     */
    public boolean isDateModificationShownInMultiSearch( )
    {
        return _bDateModificationShownInMultiSearch;
    }

    /**
     * Set whether the modification date should be shown in multi search
     * 
     * @param bDateModificationShownInMultiSearch
     *            True if the modification date should be shown in multi search, false otherwise
     */
    public void setDateModificationShownInMultiSearch( boolean bDateModificationShownInMultiSearch )
    {
        _bDateModificationShownInMultiSearch = bDateModificationShownInMultiSearch;
    }

    /**
     * Check if the modification date should be exported
     * 
     * @return True if the modification date should be exported, false otherwise
     */
    public boolean isDateModificationShownInExport( )
    {
        return _bDateModificationShownInExport;
    }

    /**
     * Set whether the modification date should be exported
     * 
     * @param bDateModificationShownInExport
     *            True if the modification date should be exported, false otherwise
     */
    public void setDateModificationShownInExport( boolean bDateModificationShownInExport )
    {
        _bDateModificationShownInExport = bDateModificationShownInExport;
    }

    /**
     * Check if the sort is ascendent
     * 
     * @return True if the sort is ascendent, false otherwise
     */
    public boolean isAscendingSort( )
    {
        return _bAscSort;
    }

    /**
     * Set whether the sort is ascendent
     * 
     * @param bAscendingSort
     *            True if the sort is ascendent, false otherwise
     */
    public void setAscendingSort( boolean bAscendingSort )
    {
        _bAscSort = bAscendingSort;
    }

    /**
     * Get the id of the entry to sort
     * 
     * @return the id of the entry to sort
     */
    public String getIdSortEntry( )
    {
        return _strSortEntryId;
    }

    /**
     * Set the id of the entry to sort
     * 
     * @param strIdSortEntry
     *            the id of the entry to sort
     */
    public void setIdSortEntry( String strIdSortEntry )
    {
        _strSortEntryId = strIdSortEntry;
    }

    /**
     * return true if the record must be sorted by ascending sort
     * 
     * @return true if the record must be sorted by ascending sort
     */
    public boolean isAscendingSortFront( )
    {
        return _bAscSortFront;
    }

    /**
     * set true if the record must be sorted by ascending sort in front office
     * 
     * @param bAscendingSort
     *            return true if the record must be sorted by ascending sort
     */
    public void setAscendingSortFront( boolean bAscendingSort )
    {
        _bAscSortFront = bAscendingSort;
    }

    /**
     * return the id of the entry used for sorted
     * 
     * @return the id of the entry used for sorted
     */
    public String getIdSortEntryFront( )
    {
        return _strSortEntryIdFront;
    }

    /**
     * set the id of the entry used for sorted
     * 
     * @param strIdSortEntry
     *            the id of the entry used for sorted
     */
    public void setIdSortEntryFront( String strIdSortEntry )
    {
        _strSortEntryIdFront = strIdSortEntry;
    }

    /**
     * Check whether record is activated
     * 
     * @return True if record is activated, false otherwise
     */
    public boolean isRecordActivated( )
    {
        return _bRecordActivated;
    }

    /**
     * Set record activated or deactivated
     * 
     * @param bRecordActivated
     *            True if record is activated, false otherwise
     */
    public void setRecordActivated( boolean bRecordActivated )
    {
        _bRecordActivated = bRecordActivated;
    }

    /**
     * return the xml of the directory
     * 
     * @param plugin
     *            the plugin
     * @param locale
     *            the locale
     * @param strListRecord
     *            the string list of record associate to the directory
     * @param strListEntry
     *            the string list of entry associate to the directory
     * @return xml
     */
    public StringBuffer getXml( Plugin plugin, Locale locale, StringBuffer strListRecord, StringBuffer strListEntry )
    {
        StringBuffer strXml = new StringBuffer( );
        HashMap<String, Object> model = new HashMap<String, Object>( );
        model.put( ATTRIBUTE_ID, String.valueOf( getIdDirectory( ) ) );
        XmlUtil.beginElement( strXml, TAG_DIRECTORY, model );
        XmlUtil.addElementHtml( strXml, TAG_TITLE, DirectoryUtils.substituteSpecialCaractersForExport( getTitle( ) ) );
        XmlUtil.addElement( strXml, TAG_CREATION_DATE, DateUtil.getDateString( getDateCreation( ), locale ) );
        XmlUtil.beginElement( strXml, TAG_LIST_ENTRY );
        strXml.append( strListEntry );
        XmlUtil.endElement( strXml, TAG_LIST_ENTRY );
        XmlUtil.beginElement( strXml, TAG_LIST_RECORD );
        strXml.append( strListRecord );
        XmlUtil.endElement( strXml, TAG_LIST_RECORD );
        XmlUtil.endElement( strXml, TAG_DIRECTORY );

        return strXml;
    }

    /**
     * Set indexed
     * 
     * @param bIsIndexed
     *            true if the directory is indexed, false otherwise
     */
    public void setIndexed( boolean bIsIndexed )
    {
        _bIsIndexed = bIsIndexed;
    }

    /**
     * Check if the directory is indexed
     * 
     * @return true if the directory is indexed, false otherwise
     */
    public boolean isIndexed( )
    {
        return _bIsIndexed;
    }

    /**
     *
     * @return true if the search operator using is OR
     */
    public boolean isSearchOperatorOr( )
    {
        return _bSearchOperatorOr;
    }

    /**
     *
     * @param bSearchOperatorOr
     *            true if the search operator using is OR
     */
    public void setSearchOperatorOr( boolean bSearchOperatorOr )
    {
        _bSearchOperatorOr = bSearchOperatorOr;
    }
}
