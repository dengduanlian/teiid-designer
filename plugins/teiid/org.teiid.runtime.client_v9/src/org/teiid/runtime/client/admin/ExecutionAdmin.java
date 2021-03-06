/*
 * JBoss, Home of Professional Open Source.
 *
 * See the LEGAL.txt file distributed with this work for information regarding copyright ownership and licensing.
 *
 * See the AUTHORS.txt file distributed with this work for a full listing of individual contributors.
 */
package org.teiid.runtime.client.admin;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.wst.server.core.IServer;
import org.jboss.dmr.ModelNode;
import org.jboss.ide.eclipse.as.core.server.v7.management.AS7ManagementDetails;
import org.jboss.ide.eclipse.as.management.core.IAS7ManagementDetails;
import org.jboss.ide.eclipse.as.management.core.JBoss7ManagerUtil;
import org.jboss.ide.eclipse.as.management.core.ModelDescriptionConstants;
import org.teiid.adminapi.Admin;
import org.teiid.core.util.ArgCheck;
import org.teiid.designer.annotation.AnnotationUtils;
import org.teiid.designer.annotation.Removed;
import org.teiid.designer.runtime.spi.EventManager;
import org.teiid.designer.runtime.spi.ExecutionConfigurationEvent;
import org.teiid.designer.runtime.spi.IExecutionAdmin;
import org.teiid.designer.runtime.spi.ITeiidConnectionInfo;
import org.teiid.designer.runtime.spi.ITeiidDataSource;
import org.teiid.designer.runtime.spi.ITeiidJdbcInfo;
import org.teiid.designer.runtime.spi.ITeiidServer;
import org.teiid.designer.runtime.spi.ITeiidTranslator;
import org.teiid.designer.runtime.spi.ITeiidTranslator.TranslatorPropertyType;
import org.teiid.designer.runtime.spi.ITeiidVdb;
import org.teiid.designer.runtime.spi.TeiidExecutionException;
import org.teiid.designer.runtime.spi.TeiidPropertyDefinition;
import org.teiid.designer.runtime.version.spi.ITeiidServerVersion;
import org.teiid.designer.runtime.version.spi.TeiidServerVersion.Version;
import org.teiid.jdbc.TeiidDriver;
import org.teiid.runtime.client.Messages;
import org.teiid.runtime.client.TeiidRuntimePlugin;
import org.teiid.runtime.client.admin.v9.Admin9Factory.AdminImpl;
import org.teiid.runtime.client.admin.v9.AdminConnectionManager;
import org.teiid.runtime.client.admin.v9.AdminUtil;
import org.teiid.runtime.client.admin.v9.VdbCacheManager;



/**
 *
 *
 * @since 8.0
 */
public class ExecutionAdmin implements IExecutionAdmin {

    private static String PLUGIN_ID = "org.teiid.runtime.client";  //$NON-NLS-1$

    private final Admin admin;
    private final EventManager eventManager;
    private final ITeiidServer teiidServer;
    private final AdminSpec adminSpec;
    private final ConnectionManager connectionManager;
    private final VdbCacheManager vdbManager;
    private final AdminConnectionManager adminConnectionManager;
    
    
    private static VdbCacheManager vdbManagerInstance;
    private static AdminConnectionManager adminConnectionManagerInstance;

    private boolean loaded = false;
    private boolean refreshing = false;

    /**
     * Constructor used for testing purposes only. 
     * 
     * @param admin the associated Teiid Admin API (never <code>null</code>)
     * @param teiidServer the server this admin belongs to (never <code>null</code>)
     * @throws Exception if there is a problem connecting the server
     */
    ExecutionAdmin(Admin admin, ITeiidServer teiidServer) throws Exception {
        ArgCheck.isNotNull(admin, "admin"); //$NON-NLS-1$
        ArgCheck.isNotNull(teiidServer, "server"); //$NON-NLS-1$
        
        this.admin = admin;
        this.teiidServer = teiidServer;
        this.adminSpec = AdminSpec.getInstance(teiidServer.getServerVersion());
        this.eventManager = teiidServer.getEventManager();
        this.adminConnectionManager = new AdminConnectionManager(((AdminImpl)this.admin).getConnection(), teiidServer.getServerVersion());
        this.connectionManager = new ConnectionManager(teiidServer, adminConnectionManager);
        this.vdbManager = new VdbCacheManager(teiidServer, admin, this.adminConnectionManager);
        
        init();
    }
    
    /**
     * Default Constructor 
     * 
     * @param teiidServer the server this admin belongs to (never <code>null</code>)
     * 
     * @throws Exception if there is a problem connecting the server
     */
    public ExecutionAdmin(ITeiidServer teiidServer) throws Exception {
        ArgCheck.isNotNull(teiidServer, "server"); //$NON-NLS-1$

        this.adminSpec = AdminSpec.getInstance(teiidServer.getServerVersion());

        this.admin = adminSpec.createAdmin(teiidServer);
        ArgCheck.isNotNull(admin, "admin"); //$NON-NLS-1$

        this.teiidServer = teiidServer;
        this.eventManager = teiidServer.getEventManager();
        this.adminConnectionManager = new AdminConnectionManager(((AdminImpl)this.admin).getConnection(), teiidServer.getServerVersion());
        this.connectionManager = new ConnectionManager(teiidServer, adminConnectionManager);
        this.vdbManager = new VdbCacheManager(teiidServer, admin, this.adminConnectionManager);

        init();
    }
    
    public static VdbCacheManager getVdbManager() {
    	return vdbManagerInstance;
    }

    public static AdminConnectionManager getAdminConnectionManager() {
    	return adminConnectionManagerInstance;
    }

    private boolean isLessThanTeiidEight() {
        ITeiidServerVersion minVersion = teiidServer.getServerVersion().getMinimumVersion();
        return minVersion.isLessThan(Version.TEIID_8_0);
    }

    @Override
    public boolean dataSourceExists( String name ) {
    	return connectionManager.dataSourceExists(name);
    }

    @Override
    public void deleteDataSource( String dsName ) throws Exception {
        // Check if exists, return false
    	String jndiName = AdminUtil.addJavaPrefix(dsName);
    	
    	// get data source
    	ITeiidDataSource existingTDS = this.connectionManager.getDataSource(jndiName);
    	ITeiidDataSource copyTDS = new TeiidDataSource(existingTDS.getDisplayName(), jndiName, existingTDS.getType());
    	this.connectionManager.deleteDataSource(jndiName);
    	
        refreshDataSources();

        // Check that local name list contains new dsName
        existingTDS = this.connectionManager.getDataSource(jndiName); //this.dataSourceByNameMap.get(dsName);
        if( existingTDS == null ) {
        	this.eventManager.notifyListeners(ExecutionConfigurationEvent.createRemoveDataSourceEvent(copyTDS));
        } 
    }

    @Override
    public void deployVdb( IFile vdbFile ) throws Exception {
        deployVdb(vdbFile, "1");
    }

    @Override
    public void deployVdb( IFile vdbFile, String version) throws Exception {
        ArgCheck.isNotNull(vdbFile, "vdbFile"); //$NON-NLS-1$
        
    	this.vdbManager.deploy(vdbFile, version);
    }
 
    @Override
    public void deployDynamicVdb( String deploymentName, InputStream inStream ) throws Exception {
        ArgCheck.isNotNull(deploymentName, "deploymentName"); //$NON-NLS-1$
        ArgCheck.isNotNull(inStream, "inStream"); //$NON-NLS-1$

        this.vdbManager.deployDynamicVdb(deploymentName, inStream);
    }
    
    @Override
    public String getSchema(String vdbName, String vdbVersion, String modelName) throws Exception {
        return vdbManager.getSchema(vdbName, vdbVersion, modelName, null, null);
    }
        
    @Override
    public void disconnect() {
    	// 
    	this.admin.close();
        this.connectionManager.disconnect();
        this.vdbManager.disconnect();
    }

    @Override
    public ITeiidDataSource getDataSource(String name) {
        return this.connectionManager.getDataSource(name); 
    }
    
    @Override
	public Collection<ITeiidDataSource> getDataSources() {
        return this.connectionManager.getDataSources();
    }

    @Override
	public Set<String> getDataSourceTypeNames() {
        return this.connectionManager.getDataSourceTypeNames();
    }

    /**
     * @return the event manager (never <code>null</code>)
     */
    public EventManager getEventManager() {
        return this.eventManager;
    }

    @Override
    public ITeiidDataSource getOrCreateDataSource( String displayName,
                                                  String jndiName,
                                                  String dataSourceType,
                                                  Properties properties ) throws Exception {
        ArgCheck.isNotEmpty(displayName, "displayName"); //$NON-NLS-1$
        ArgCheck.isNotEmpty(jndiName, "jndiName"); //$NON-NLS-1$
        ArgCheck.isNotEmpty(dataSourceType, "dataSourceType"); //$NON-NLS-1$
        ArgCheck.isNotEmpty(properties, "properties"); //$NON-NLS-1$
        
        if(! AdminUtil.hasJavaPrefix(jndiName) ) {
            throw new TeiidExecutionException(
            		9996,
            		"JNDI name : " + jndiName + " does not include the java:/ prefix");
        }

        // Check if exists, return false
        if (dataSourceExists(jndiName)) {
            ITeiidDataSource tds = this.connectionManager.getDataSource(jndiName); //dataSourceByNameMap.get(dsName);
            if (tds != null) {
                return tds;
            }
        }
        
        boolean isJdbc = "connector-jdbc".equals(dataSourceType);

        // For JDBC types, find the matching installed driver.  This is done currently by matching
        // the profile driver classname to the installed driver classname
        String connProfileDriverClass = properties.getProperty("driver-class");  //$NON-NLS-1$
        if(isJdbc) {
            // List of driver jars on the connection profile
            String jarList = properties.getProperty("jarList");  //$NON-NLS-1$
            
            // Get first driver name with the driver class that matches the connection profile
            String dsNameMatch = getDSMatchForDriverClass(connProfileDriverClass);
            
            // If a matching datasource was found, set typename
            if(dsNameMatch!=null) {
            	dataSourceType=dsNameMatch;
            // No matching datasource, attempt to deploy the driver if jarList is populated.
            } else if(jarList!=null && jarList.trim().length()>0) {
                // Try to deploy the jars
                deployJars(this.admin,jarList);
                
                refresh();
                
                // Retry the name match after deployment.
                dsNameMatch = getDSMatchForDriverClass(connProfileDriverClass);
                if(dsNameMatch!=null) {
                	dataSourceType=dsNameMatch;
                }
            }
        }
        // Verify the "typeName" exists.
        if (!this.connectionManager.dataSourceTypeExists(dataSourceType)) {
            if(isJdbc) {  //$NON-NLS-1$
                throw new TeiidExecutionException(
                		ITeiidDataSource.ERROR_CODES.JDBC_DRIVER_SOURCE_NOT_FOUND,
                		Messages.getString(Messages.ExecutionAdmin.jdbcSourceForClassNameNotFound, connProfileDriverClass, getServer()));
            } else {
                throw new TeiidExecutionException(
                		ITeiidDataSource.ERROR_CODES.DATA_SOURCE_TYPE_DOES_NOT_EXIST_ON_SERVER,
                		Messages.getString(Messages.ExecutionAdmin.dataSourceTypeDoesNotExist, dataSourceType, getServer()));
            }
        }

        if("teiid".equals(dataSourceType) ) {
        	isJdbc = true;
        }
        this.connectionManager.createDataSource(jndiName, dataSourceType, isJdbc, properties);
        
        refreshDataSources();

        // Check that local name list contains new dsName
        ITeiidDataSource tds = this.connectionManager.getDataSource(jndiName);
        if( tds != null ) {
        	this.eventManager.notifyListeners(ExecutionConfigurationEvent.createAddDataSourceEvent(tds));
        	return tds;
        } 

        // We shouldn't get here if data source was created
        throw new TeiidExecutionException(
        		ITeiidDataSource.ERROR_CODES.DATA_SOURCE_COULD_NOT_BE_CREATED,
        		Messages.getString(Messages.ExecutionAdmin.errorCreatingDataSource, jndiName, dataSourceType));
    }

    /**
     * Look for an installed driver that has the driverClass which matches the supplied driverClass name.
     * 
     * @param requestDriverClass the driver class to match
     * @return the name of the matching driver, null if not found
     */
    private String getDSMatchForDriverClass(String requestDriverClass) throws Exception {
        if (requestDriverClass == null)
            return null;

        if (!getServer().isParentConnected())
            return null;

        ModelNode request = new ModelNode();
        request.get(ModelDescriptionConstants.OP).set("installed-drivers-list"); //$NON-NLS-1$

        ModelNode address = new ModelNode();
        address.add(ModelDescriptionConstants.SUBSYSTEM, "datasources"); //$NON-NLS-1$
        request.get(ModelDescriptionConstants.OP_ADDR).set(address);

        try {
            String requestString = request.toJSONString(true);
            IServer parentServer = getServer().getParent();

            //
            // Add the timeout to a properties map
            //
            int timeout = teiidServer.getParentRequestTimeout();
            Map<String, Object> props = new HashMap<String, Object>();
            props.put(IAS7ManagementDetails.PROPERTY_TIMEOUT, timeout);

            AS7ManagementDetails as7ManagementDetails = new AS7ManagementDetails(parentServer, props);
            String resultString = JBoss7ManagerUtil.getService(parentServer).execute(as7ManagementDetails, requestString);
            ModelNode operationResult = ModelNode.fromJSONString(resultString);

            List<ModelNode> driverList = operationResult.asList();
            for (ModelNode driver : driverList) {
                String driverClassName = driver.get("driver-class-name").asString(); //$NON-NLS-1$
                String driverName = driver.get("driver-name").asString(); //$NON-NLS-1$

                if (requestDriverClass.equalsIgnoreCase(driverClassName)) return driverName;
            }

        } catch (Exception ex) {
            // Failed to get mapping
            TeiidRuntimePlugin.logError(getClass().getSimpleName(), ex, Messages.getString(Messages.ExecutionAdmin.failedToGetDriverMappings, requestDriverClass));
        }

        return null;
    }
    
    /*
     * Deploy all jars in the supplied jarList
     * @param admin the Admin instance
     * @param jarList the colon-separated list of jar path locations
     */
    private void deployJars(Admin admin, String jarList) {
        // Path Entries are separated by the file system path separator (WINDOWS = ';', LINUX = ':')
    	String splitter = "[" + File.pathSeparatorChar + "]"; //$NON-NLS-1$ //$NON-NLS-2$

        String[] jarPathStrs = jarList.split(splitter); 

        // Attempt to deploy each jar
        for(String jarPathStr: jarPathStrs) {
            File theFile = new File(jarPathStr);
            if(theFile.exists()) {
                if(theFile.canRead()) {
                    String fileName = theFile.getName();
                    InputStream iStream = null;
                    try {
                        iStream = new FileInputStream(theFile);
                    } catch (FileNotFoundException ex) {
                        TeiidRuntimePlugin.logError(getClass().getSimpleName(), ex, Messages.getString(Messages.ExecutionAdmin.jarDeploymentJarNotFound, theFile.getPath()));
                        continue;
                    }
                    try {
                        adminSpec.deploy(admin, fileName, iStream);
                        
                        refresh();
                        
                    } catch (Exception ex) {
                        // Jar deployment failed
                        TeiidRuntimePlugin.logError(getClass().getSimpleName(), ex, Messages.getString(Messages.ExecutionAdmin.jarDeploymentFailed, theFile.getPath()));
                    }

                    if( iStream != null ) {
                    	try {
							iStream.close();
						} catch (IOException e) {
							e.printStackTrace();
						}
                    }
                } else {
                    // Could not read the file
                    TeiidRuntimePlugin.logError(getClass().getSimpleName(), Messages.getString(Messages.ExecutionAdmin.jarDeploymentJarNotReadable, theFile.getPath()));
                }

            } else {
                // The file was not found
                TeiidRuntimePlugin.logError(getClass().getSimpleName(), Messages.getString(Messages.ExecutionAdmin.jarDeploymentJarNotFound, theFile.getPath()));
            }

        }
    }
    
    @Override
    public void deployDriver(File jarOrRarFile) throws Exception {
        if(jarOrRarFile.exists()) {
            if(jarOrRarFile.canRead()) {
                String fileName = jarOrRarFile.getName();
                InputStream iStream = null;
                try {
                    iStream = new FileInputStream(jarOrRarFile);
                } catch (FileNotFoundException ex) {
                    TeiidRuntimePlugin.logError(getClass().getSimpleName(), ex, Messages.getString(Messages.ExecutionAdmin.jarDeploymentJarNotFound, jarOrRarFile.getPath()));
                    throw ex;
                }
                try {
                    adminSpec.deploy(admin, fileName, iStream);
                    
                    refresh();
                    
                } catch (Exception ex) {
                    // Jar deployment failed
                    TeiidRuntimePlugin.logError(getClass().getSimpleName(), ex, Messages.getString(Messages.ExecutionAdmin.jarDeploymentFailed, jarOrRarFile.getPath()));
                    throw ex;
                } finally {
                    if( iStream != null ) {
                    	iStream.close();
                    }
                }

            } else {
                // Could not read the file
                TeiidRuntimePlugin.logError(getClass().getSimpleName(), Messages.getString(Messages.ExecutionAdmin.jarDeploymentJarNotReadable, jarOrRarFile.getPath()));
            }
        } else {
            // The file was not found
            TeiidRuntimePlugin.logError(getClass().getSimpleName(), Messages.getString(Messages.ExecutionAdmin.jarDeploymentJarNotFound, jarOrRarFile.getPath()));
        }
    }

    /**
     * @return the server who owns this admin object (never <code>null</code>)
     */
    public ITeiidServer getServer() {
        return this.teiidServer;
    }

    @Override
    public ITeiidTranslator getTranslator( String name ) {
        ArgCheck.isNotEmpty(name, "name"); //$NON-NLS-1$
        return this.connectionManager.getTranslator(name); 
    }

    @Override
    public Collection<ITeiidTranslator> getTranslators() {
        return this.connectionManager.getTranslators();
    }

    @Override
    public Set<String> getDataSourceTemplateNames() throws Exception {
        return this.connectionManager.getDataSourceTypeNames();
    }
    
	@Override
    public Collection<TeiidPropertyDefinition> getTemplatePropertyDefns(String templateName) throws Exception {
    	return this.connectionManager.getTemplatePropertyDefns(templateName);
    }

    /*
     * (non-Javadoc)
     * @see org.teiid.designer.runtime.spi.IExecutionAdmin#getDataSourceProperties(java.lang.String)
     */
    @Override
    public Properties getDataSourceProperties(String name) throws Exception {
        if (isLessThanTeiidEight()) {
            // Teiid 7.7.x does not support
            return null;
        }
        ITeiidDataSource ds = getDataSource(name);
        if( ds != null ) {
        	return ds.getProperties();
        }
        return new Properties();
    }

    @Override
    public ITeiidVdb getVdb( String name ) {
        ArgCheck.isNotEmpty(name, "name"); //$NON-NLS-1$
        return this.vdbManager.getVdb(name);
    }
    
    @Override
    public boolean hasVdb(String name) throws Exception {
        return getVdb(name) != null;
    }
    
    @Override
    public boolean isVdbActive(String vdbName) throws Exception {
        if (! hasVdb(vdbName))
            return false;
        
        return getVdb(vdbName).isActive();
    }
    
    @Override
    public boolean isVdbLoading(String vdbName) throws Exception {
        if (! hasVdb(vdbName))
            return false;
        
        return getVdb(vdbName).isLoading();
    }
    
    @Override
    public boolean isRefreshing() throws Exception {
    	return this.refreshing;
    }
    
    @Override
    public boolean hasVdbFailed(String vdbName) throws Exception {
        if (! hasVdb(vdbName))
            return false;
        
        return getVdb(vdbName).hasFailed();
    }
    
    @Override
    public boolean wasVdbRemoved(String vdbName) throws Exception {
        if (! hasVdb(vdbName))
            return false;
        
        return getVdb(vdbName).wasRemoved();
    }
    
    @Override
    public List<String> retrieveVdbValidityErrors(String vdbName) throws Exception {
        if (! hasVdb(vdbName))
            return Collections.emptyList();
        
        return getVdb(vdbName).getValidityErrors();
    }

    @Override
    public Collection<ITeiidVdb> getVdbs() {
        return this.vdbManager.getVdbs();
    }
    
    private void init() throws Exception {
        this.connectionManager.init();
        this.vdbManager.init();
        
        vdbManagerInstance = this.vdbManager;
        adminConnectionManagerInstance = this.adminConnectionManager;
    }

    private void internalSetPropertyValue( ITeiidTranslator translator,
                                           String propName,
                                           String value,
                                           TranslatorPropertyType type,
                                           boolean notify ) throws Exception {
        if (translator.isValidPropertyValue(propName, value, type) == null) {
            String oldValue = translator.getPropertyValue(propName, type);

            // don't set if value has not changed
            if (oldValue == null) {
                if (value == null) return;
            } else if (oldValue.equals(value)) return;

            if (notify) {
                // TODO: Will we ever update Translator properties in TEIID Server?
                // this.eventManager.notifyListeners(ExecutionConfigurationEvent.createUpdateConnectorEvent(translator));
            }
        } else {
            throw new Exception(Messages.getString(Messages.ExecutionAdmin.invalidPropertyValue, value, propName));
        }
    }

    /**
     * @throws Exception if refreshing admin connection fails
     */
    @Override
    public void connect() throws Exception {
        if (!this.loaded) {
            refresh();
            this.loaded = true;
        }
    }

    /**
     * Refreshes the cached lists and maps of current Teiid objects
     * @throws Exception if refreshing admin connection fails
     */
    public void refresh() throws Exception {
    	refreshing = true;
    	
        // populate data source type names
    	Exception resultException = null;
    	
    	try {
	
	    	try {
	    		refreshDataSources();
	    	} catch (Exception ex) {
	    		resultException = ex;
	    	}
	    	
	    	try {
	    		refreshDataSourceTypes();
	    	} catch (Exception ex) {
	    		resultException = ex;
	    	}
	        
	        // populate translator map
	    	try {
	    		refreshTranslators();
	    	} catch (Exception ex) {
	    		resultException = ex;
	    	}
	
	        // populate VDBs and source bindings
	    	try {
	    		refreshVDBs();
	    	} catch (Exception ex) {
	    		resultException = ex;
	    	}

    	} finally  {
    		// Wrapped in try-catch so that any exception will still set refreshLoaded = true;
        	refreshing = false;
    	}
    	
    	if(resultException!=null) throw resultException;
  
        // notify listeners
        this.eventManager.notifyListeners(ExecutionConfigurationEvent.createServerRefreshEvent(this.teiidServer));
    }
    
    protected void refreshDataSources() throws Exception {
    	this.connectionManager.refreshDataSources();
    }

    /**
     * Refreshes the local collection of Translators on the referenced Teiid server.
     * 
     * @param translators
     * @throws Exception
     */
    protected void refreshTranslators() throws Exception {
    	this.connectionManager.refreshTranslators();
    }

    protected void refreshVDBs() throws Exception {
    	this.vdbManager.refresh();
    }
    
    protected void refreshDataSourceTypes() throws Exception {
    	this.connectionManager.refreshDataSourceTypes();
    }

    /**
     * @param translator the translator whose properties are being changed (never <code>null</code>)
     * @param changedProperties a collection of properties that have changed (never <code>null</code> or empty)
     * @param type the translator property type
     * @throws Exception if there is a problem changing the properties
     * @since 7.0
     */
    public void setProperties( ITeiidTranslator translator,
                               Properties changedProperties,
                               TranslatorPropertyType type) throws Exception {
        ArgCheck.isNotNull(translator, "translator"); //$NON-NLS-1$
        ArgCheck.isNotNull(changedProperties, "changedProperties"); //$NON-NLS-1$
        ArgCheck.isNotEmpty(changedProperties.entrySet(), "changedProperties"); //$NON-NLS-1$

        if (changedProperties.size() == 1) {
            String name = changedProperties.stringPropertyNames().iterator().next();
            setPropertyValue(translator, name, changedProperties.getProperty(name), type);
        } else {

            for (String name : changedProperties.stringPropertyNames()) {
                internalSetPropertyValue(translator, name, changedProperties.getProperty(name), type, false);
            }
            // this.eventManager.notifyListeners(ExecutionConfigurationEvent.createUpdateConnectorEvent(translator));
        }
    }

    /**
     * @param translator the translator whose property is being changed (never <code>null</code>)
     * @param propName the name of the property being changed (never <code>null</code> or empty)
     * @param value the new value
     * @param type the translator property type
     * @throws Exception if there is a problem setting the property
     * @since 7.0
     */
    public void setPropertyValue( ITeiidTranslator translator,
                                  String propName,
                                  String value,
                                  TranslatorPropertyType type) throws Exception {
        ArgCheck.isNotNull(translator, "translator"); //$NON-NLS-1$
        ArgCheck.isNotEmpty(propName, "propName"); //$NON-NLS-1$
        ArgCheck.isNotEmpty(value, "value"); //$NON-NLS-1$
        internalSetPropertyValue(translator, propName, value, type, true);
    }

    @Override
    public void undeployVdb( String vdbName) throws Exception {
        ITeiidVdb vdb = getVdb(vdbName);
        if(vdb!=null) {
        	String deploymentName = vdb.getPropertyValue("deployment-name"); //$NON-NLS-1$
        	if( deploymentName != null ) {
        		adminSpec.undeploy(admin, deploymentName, vdb.getVersion());
        	} else {
        		throw new Exception(Messages.getString(Messages.ExecutionAdmin.cannotUndeployVdbNoDeploymentName, vdbName));
            }
        }
        vdb = getVdb(vdbName);

        refreshVDBs();

        if (vdb != null) {
        	this.eventManager.notifyListeners(ExecutionConfigurationEvent.createUnDeployVDBEvent(vdb.getName()));
        }
    }
    
    @Override
    public void undeployDynamicVdb( String vdbName) throws Exception {
        ITeiidVdb vdb = getVdb(vdbName);
        if(vdb!=null) {
        	adminSpec.undeploy(admin, appendDynamicVdbSuffix(vdbName), vdb.getVersion());
        }
        vdb = getVdb(vdbName);

        refreshVDBs();

        if (vdb == null) {

        } else {
            this.eventManager.notifyListeners(ExecutionConfigurationEvent.createUnDeployVDBEvent(vdb.getName()));
        }
    }

    /**
     * 
     * @param vdbName the vdb name
     * @param vdbVersion the vdb version
     * @throws Exception if undeploying vdb fails
     */
    public void undeployVdb( String vdbName, String vdbVersion ) throws Exception {
        adminSpec.undeploy(admin, appendVdbExtension(vdbName), vdbVersion);
        ITeiidVdb vdb = getVdb(vdbName);

        refreshVDBs();

        if (vdb == null) {

        } else {
            this.eventManager.notifyListeners(ExecutionConfigurationEvent.createUnDeployVDBEvent(vdb.getName()));
        }
    }
    
    /**
     * Append the vdb file extension to the vdb name 
     * if not already appended.
     * 
     * @param vdbName
     * @return
     */
    private String appendVdbExtension(String vdbName) {
        if (vdbName.endsWith(ITeiidVdb.VDB_EXTENSION))
            return vdbName;
        
        return vdbName + ITeiidVdb.VDB_DOT_EXTENSION;
    }
    
    /**
     * Append the suffix for dynamic VDB to the vdb name if not already appended.
     * 
     * @param vdbName
     * @return
     */
    private String appendDynamicVdbSuffix(String vdbName) {
        if (vdbName.endsWith(ITeiidVdb.DYNAMIC_VDB_SUFFIX))
            return vdbName;
        
        return vdbName + ITeiidVdb.DYNAMIC_VDB_SUFFIX;
    }
    
    @Override
    public IStatus ping(PingType pingType) {
        String msg = Messages.getString(Messages.ExecutionAdmin.cannotConnectToServer, teiidServer.getTeiidAdminInfo().getUsername());
        try {
            if (this.admin == null)
                throw new Exception(msg);
            
            switch(pingType) {
                case JDBC:
                    return pingJdbc();
                case ADMIN:
                default:
                    return pingAdmin();
            }
        }
        catch (Exception ex) {
            return new Status(IStatus.ERROR, PLUGIN_ID, msg, ex);
        }
    }
    
    private IStatus pingAdmin() throws Exception {
        admin.getSessions();
        return Status.OK_STATUS;
    }
    
    private IStatus pingJdbc() {
        String host = teiidServer.getHost();
        ITeiidJdbcInfo teiidJdbcInfo = teiidServer.getTeiidJdbcInfo();
        
        String protocol = ITeiidConnectionInfo.MM;
        if (teiidJdbcInfo.isSecure())
            protocol = ITeiidConnectionInfo.MMS;

        Connection teiidJdbcConnection = null;
        String url = "jdbc:teiid:ping@" + protocol + host + ':' + teiidJdbcInfo.getPort(); //$NON-NLS-1$
        
        try {
            adminSpec.deploy(admin, PING_VDB, new ByteArrayInputStream(adminSpec.getTestVDB().getBytes()));
            
            try{
                String urlAndCredentials = url + ";";  //$NON-NLS-1$             
                TeiidDriver teiidDriver = TeiidDriver.getInstance();
                teiidDriver.setTeiidVersion(teiidServer.getServerVersion());
                Properties props = new Properties();
                props.put("user", teiidJdbcInfo.getUsername());
                props.put("password", teiidJdbcInfo.getPassword());
                teiidJdbcConnection = teiidDriver.connect(urlAndCredentials, props);
               //pass
            } catch(SQLException ex){
                String msg = Messages.getString(Messages.ExecutionAdmin.serverDeployUndeployProblemPingingTeiidJdbc, url);
                return new Status(IStatus.ERROR, PLUGIN_ID, msg, ex);
            } finally {
                adminSpec.undeploy(admin, PING_VDB, "1");
                
                if( teiidJdbcConnection != null ) {
                    teiidJdbcConnection.close();
                }
            }
        } catch (Exception ex) {
            String msg = Messages.getString(Messages.ExecutionAdmin.serverDeployUndeployProblemPingingTeiidJdbc, url);
            return new Status(IStatus.ERROR, PLUGIN_ID, msg, ex);
        }
        
        return Status.OK_STATUS;
    }
    
    @Override
    public String getAdminDriverPath() {
        return Admin.class.getProtectionDomain().getCodeSource().getLocation().getFile();
    }
    
    @Override
    public Driver getTeiidDriver(String driverClass) throws Exception {
        Class<?> klazz = getClass().getClassLoader().loadClass(driverClass);
        Object driver = klazz.newInstance();
        if (driver instanceof Driver)
            return (Driver) driver;
        
        throw new Exception(Messages.getString(Messages.ExecutionAdmin.cannotLoadDriverClass, driverClass));
    }

    @Override
    @Deprecated
    @Removed(Version.TEIID_8_0)
    public void mergeVdbs( String sourceVdbName, int sourceVdbVersion, 
                                            String targetVdbName, int targetVdbVersion ) throws Exception {
        if (!AnnotationUtils.isApplicable(getClass().getMethod("mergeVdbs", String.class, int.class, String.class, int.class), getServer().getServerVersion()))  //$NON-NLS-1$
            throw new UnsupportedOperationException(Messages.getString(Messages.ExecutionAdmin.mergeVdbUnsupported));

        admin.mergeVDBs(sourceVdbName, sourceVdbVersion, targetVdbName, targetVdbVersion);        
    }
}
