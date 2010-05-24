/*
 * JBoss, Home of Professional Open Source.
 *
 * See the LEGAL.txt file distributed with this work for information regarding copyright ownership and licensing.
 *
 * See the AUTHORS.txt file distributed with this work for a full listing of individual contributors.
 */
package com.metamatrix.modeler.internal.jdbc;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import javax.sql.DataSource;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.datatools.connectivity.IConnection;
import org.eclipse.datatools.connectivity.IConnectionProfile;
import org.eclipse.datatools.connectivity.IProfileListener;
import org.eclipse.datatools.connectivity.ProfileManager;
import org.eclipse.datatools.connectivity.drivers.DriverInstance;
import org.eclipse.datatools.connectivity.drivers.DriverManager;

import com.metamatrix.core.util.CoreArgCheck;
import com.metamatrix.modeler.core.ModelerCore;
import com.metamatrix.modeler.jdbc.JdbcDriver;
import com.metamatrix.modeler.jdbc.JdbcDriverContainer;
import com.metamatrix.modeler.jdbc.JdbcDriverProperty;
import com.metamatrix.modeler.jdbc.JdbcException;
import com.metamatrix.modeler.jdbc.JdbcFactory;
import com.metamatrix.modeler.jdbc.JdbcManager;
import com.metamatrix.modeler.jdbc.JdbcPlugin;
import com.metamatrix.modeler.jdbc.JdbcSource;
import com.metamatrix.modeler.jdbc.JdbcSourceContainer;
import com.metamatrix.modeler.jdbc.JdbcSourceProperty;
import com.metamatrix.modeler.jdbc.impl.JdbcFactoryImpl;

/**
 * JdbcManagerImpl
 */
public class JdbcManagerImpl implements JdbcManager {

    // ===========================================================================================================================
    // Constants

    public static final int MISSING_DRIVER_CLASS = 1001;
    public static final int VALID_SOURCE = 1002;
    public static final int NAME_NOT_SPECIFIED = 1003;
    public static final int NAME_MUST_BEGIN_WITH_LETTER_OR_NUMBER = 1004;
    public static final int NO_AVAILABLE_DRIVER_CLASS_NAMES = 1005;
    public static final int PREFERRED_NOT_SPECIFIED = 1006;
    public static final int PREFERRED_NOT_IN_AVAILABLE = 1007;
    public static final int NO_JARS_SPECIFIED = 1008;
    public static final int VALID_DRIVER = 1009;
    public static final int ILLEGAL_CHAR_AT_START_OF_CLASS_NAME = 1010;
    public static final int ILLEGAL_CHAR_IN_CLASS_NAME = 1011;
    public static final int JAR_FILE_URI_NOT_SPECIFIED = 1012;
    public static final int JAR_FILE_DOESNT_EXIST = 1013;
    public static final int MALFORMED_URL = 1014;
    public static final int PROPERTY_NAME_MISSING = 1015;
    public static final int PROPERTY_VALUE_MISSING = 1016;
    public static final int ERROR_BUILDING_CLASSLOADER = 1017;
    public static final int UNABLE_TO_FIND_DRIVER = 1018;
    public static final int ERROR_CHECKING_DRIVER_CLASS = 1019;
    public static final int URL_NOT_SPECIFIED = 1020;
    public static final int URL_MUST_START_WITH_JDBC = 1021;
    public static final int ERROR_FINDING_DRIVER_CLASS = 1022;
    public static final int MALFORMED_URL_SYNTAX = 1023;
    public static final int INVALID_CONNECTION_PROFILE = 1024;
    public static final int MISSING_CONNECTION_PROFILE = 1025;

    public static final int AVAILABLE_CLASSES_COMPLETE = 1100;
    public static final int AVAILABLE_CLASSES_WITH_WARNINGS = 1102;
    public static final int AVAILABLE_CLASSES_WITH_ERRORS = 1103;
    public static final int AVAILABLE_CLASSES_WITH_WARNINGS_AND_ERRORS = 1104;

    private static final String PID = JdbcPlugin.PLUGIN_ID;

    /**
     * @since 5.0
     */
    public static final String JDBC_MODEL = "jdbcModel" + ModelerCore.MODEL_FILE_EXTENSION; //$NON-NLS-1$

    // ===========================================================================================================================
    // Static Variables

    private static JdbcManager shared;

    // ===========================================================================================================================
    // Static Model Methods

    /**
     * @param name
     * @param folder
     * @param container
     * @return The shared instance of this class.
     * @throws JdbcException
     * @since 5.0
     */
    public static JdbcManager create( final String name ) throws JdbcException {
        if (JdbcManagerImpl.shared == null) {
            // Create and start JDBC manager (which loads JDBC driver model)
            final JdbcManagerImpl mgr = new JdbcManagerImpl(name);
            mgr.start();
            JdbcManagerImpl.shared = mgr;
        }
        return JdbcManagerImpl.shared;
    }

    /**
     * @return The shared instance of this class.
     * @since 5.0
     */
    public static JdbcManager get() {
        return JdbcManagerImpl.shared;
    }

    // ===========================================================================================================================
    // Variables

    private final String name;
    private JdbcDriverContainer drivers;
    private JdbcSourceContainer sources;
    private final Object driversLock = new Object();
    private final Object sourcesLock = new Object();
    private ProfileManager profileManager;
    private DriverManager driverManager;
    private boolean sourcesUpdated;
	private ProfileListener profileListener;

    // ===========================================================================================================================
    // Constructors

    /**
     * Construct an instance of JdbcManagerImpl. This form of the contructor is useful when the resource is already opened and
     * known.
     * 
     * @param name the name of this manager; may not be null or zero-length
     */
    public JdbcManagerImpl( final String name ) {
        super();
        CoreArgCheck.isNotNull(name);
        CoreArgCheck.isNotZeroLength(name);
        this.name = name;
    }

    /* (non-Javadoc)
     * @see com.metamatrix.modeler.jdbc.JdbcManager#getName()
     */
    public String getName() {
        return this.name;
    }

    /**
     * This method is not synchronized and is not thread safe.
     */
    public void start() throws JdbcException {
        profileManager = ProfileManager.getInstance();
		profileListener = new ProfileListener();
		profileManager.addProfileListener(profileListener);
        driverManager = DriverManager.getInstance();
    }

    /**
     * This method is not synchronized and is not thread safe.
     */
    public void shutdown() {
		profileManager.removeProfileListener(profileListener);
        profileManager = null;
        driverManager = null;
        sources = null; // no need to synchronize since nulling reference is atomic
        drivers = null; // no need to synchronize since nulling reference is atomic
    }

    /* (non-Javadoc)
     * @see com.metamatrix.modeler.jdbc.JdbcManager#hasChanges()
     */
    public boolean hasChanges() {
        return true;
    }

    /* (non-Javadoc)
     * @see com.metamatrix.modeler.jdbc.JdbcManager#saveChanges(org.eclipse.core.runtime.IProgressMonitor)
     */
    public void saveChanges( IProgressMonitor monitor ) throws IOException {
        // If there are no changes, then return
        if (!hasChanges()) {
            return;
        }
    }

    /* (non-Javadoc)
     * @see com.metamatrix.modeler.jdbc.JdbcManager#reload(org.eclipse.core.runtime.IProgressMonitor)
     */
    public void reload( IProgressMonitor monitor ) throws JdbcException {
        shutdown();
        start();
    }

    /* (non-Javadoc)
     * @see com.metamatrix.modeler.jdbc.JdbcManager#getJdbcDrivers()
     */
    public List getJdbcDrivers() {
        if (drivers == null) {
            synchronized (driversLock) {
                if (drivers == null) {
                    JdbcFactoryImpl factory = new JdbcFactoryImpl();
                    drivers = factory.createJdbcDriverContainer();
                    DriverInstance[] tempDrivers = driverManager.getDriverInstancesByCategory("org.eclipse.datatools.connectivity.db.category");
                    for (int i = 0; i < tempDrivers.length; i++) {
                        DriverInstance driverInstance = tempDrivers[i];
                        JdbcDriver driver = factory.createJdbcDriver();
                        driver.setName(driverInstance.getName());
                        driver.setPreferredDriverClassName(driverInstance.getNamedPropertyByID("org.eclipse.datatools.connectivity.db.driverClass"));
                        driver.setUrlSyntax(driverInstance.getNamedPropertyByID("org.eclipse.datatools.connectivity.db.URL"));
                        driver.setJdbcDriverContainer(drivers);
                        drivers.getJdbcDrivers().add(driver);
                    }
                }
            }
        }
        return drivers.getJdbcDrivers();
    }

    /* (non-Javadoc)
     * @see com.metamatrix.modeler.jdbc.JdbcManager#getJdbcSources()
     */
    public List getJdbcSources() {
        if (sources == null || sourcesUpdated) {
            synchronized (sourcesLock) {
                if (sources == null) {
                    JdbcFactoryImpl factory = new JdbcFactoryImpl();
                    sources = factory.createJdbcSourceContainer();
                    IConnectionProfile[] tempProfiles = profileManager.getProfilesByCategory("org.eclipse.datatools.connectivity.db.category");
                    for (int i = 0; i < tempProfiles.length; i++) {
                        IConnectionProfile profile = tempProfiles[i];
                        JdbcSource source = getJdbcSource(profile);
                        source.setJdbcSourceContainer(sources);
                        sources.getJdbcSources().add(source);
                    }
                }
            }
        }
        return sources.getJdbcSources();
    }

    public JdbcSource getJdbcSource( IConnectionProfile profile ) {
        JdbcSource source = new JdbcFactoryImpl().createJdbcSource();
        source.setName(profile.getName());
        Properties props = profile.getBaseProperties();
        source.setDriverClass(props.getProperty("org.eclipse.datatools.connectivity.db.driverClass"));
        source.setUsername(props.getProperty("org.eclipse.datatools.connectivity.db.username"));
        source.setUrl(props.getProperty("org.eclipse.datatools.connectivity.db.URL"));
        String password = props.getProperty("org.eclipse.datatools.connectivity.db.password");
        if (null != password) {
            source.setPassword(password);
        }
        String driverID = props.getProperty("org.eclipse.datatools.connectivity.driverDefinitionID");
        DriverInstance driver = driverManager.getDriverInstanceByID(driverID);
        source.setDriverName(driver.getName());
        return source;
    }

    /* (non-Javadoc)
     * @see com.metamatrix.modeler.jdbc.JdbcManager#getFactory()
     */
    public JdbcFactory getFactory() {
        return JdbcFactory.eINSTANCE;
    }

    public Connection createConnection( final JdbcSource jdbcSource,
                                        final String password,
                                        final IProgressMonitor monitor ) throws JdbcException, SQLException {
        // TODO: the monitor
        return createConnection(jdbcSource, password);
    }

    @Override
    public Connection createConnection( JdbcSource jdbcSource,
                                        String password ) throws JdbcException, SQLException {
        IConnectionProfile profile = profileManager.getProfileByName(jdbcSource.getName());
        if (null == profile) {
            throw new JdbcException("ConnectionProfile " + jdbcSource.getName() + " cannot be found");
        }

        String factoryId = profile.getProvider().getConnectionFactory("java.sql.Connection").getId();
        // override the pw in the ConnectionProfile with one supplied in the importer.
        // IConnection connection = profile.createConnection(factoryId, jdbcSource.getUsername(), password);
        IConnection connection = profile.createConnection(factoryId);

        Connection sqlConnection = (Connection)connection.getRawConnection();
        if (null == sqlConnection || sqlConnection.isClosed()) {
            Throwable e = connection.getConnectException();
            throw new JdbcException(e == null ? "Unspecified connection error" : e.getMessage());
        }
        return (Connection)connection.getRawConnection();
    }

    /**
     * @param jdbcSource
     * @return
     */
    protected Properties createProperties( final JdbcSource jdbcSource ) {
        final Properties props = new Properties();
        final Iterator iter = jdbcSource.getProperties().iterator();
        while (iter.hasNext()) {
            final JdbcSourceProperty prop = (JdbcSourceProperty)iter.next();
            props.put(prop.getName().toLowerCase(), prop.getValue());
        }
        return props;
    }

    /**
     * @param dataSource
     */
    protected void setProperties( final Properties props,
                                  final DataSource dataSource ) throws JdbcException {
        final Method[] methods = dataSource.getClass().getMethods();
        for (int i = 0; i < methods.length; i++) {
            final Method method = methods[i];
            final String methodName = method.getName();
            // If setter ...
            if (methodName.startsWith("set") && method.getParameterTypes().length == 1) { //$NON-NLS-1$
                // Get the property name
                final String propertyName = methodName.substring(3); // remove the "set"
                String propertyValue = (String)props.get(propertyName.toLowerCase());
                if (propertyValue != null) {
                    final Class argType = method.getParameterTypes()[0];
                    final Object[] params = new Object[1];
                    if (argType == Integer.TYPE) {
                        params[0] = Integer.decode(propertyValue);
                    } else if (argType == Boolean.TYPE) {
                        params[0] = Boolean.valueOf(propertyValue);
                    } else if (argType == String.class) {
                        params[0] = propertyValue;
                    }

                    // Actually set the property ...
                    try {
                        method.invoke(dataSource, params);
                    } catch (Throwable e) {
                        final Object[] msgParams = new Object[] {propertyName, propertyValue};
                        final String msg = JdbcPlugin.Util.getString("JdbcManagerImpl.Unable_to_set_property_to_value", msgParams); //$NON-NLS-1$
                        throw new JdbcException(msg);
                    }
                }
            }
        }
    }

    protected JdbcDriverProperty[] getPropertyDescriptions( final Driver driver,
                                                            final JdbcSource jdbcSource ) throws SQLException {
        CoreArgCheck.isNotNull(driver);
        CoreArgCheck.isNotNull(jdbcSource);

        DriverPropertyInfo[] propInfo = null;
        final String url = jdbcSource.getUrl() != null ? jdbcSource.getUrl() : ""; //$NON-NLS-1$
        try {
            propInfo = driver.getPropertyInfo(url, null);
        } catch (RuntimeException err) {
            // Some drivers (Sun's ODBC-JDBC) throw null pointer exceptions ...

            // Try again, but with an empty properties ...
            try {
                propInfo = driver.getPropertyInfo(url, new Properties());
            } catch (RuntimeException err2) {
                // Okay, give up
            }
        }
        if (propInfo == null || propInfo.length == 0) {
            return new JdbcDriverProperty[] {};
        }

        final List props = new ArrayList(propInfo.length);
        for (int i = 0; i < propInfo.length; i++) {
            final DriverPropertyInfo info = propInfo[i];
            final JdbcDriverProperty prop = new JdbcDriverProperty(info);
            props.add(prop);
        }

        return (JdbcDriverProperty[])props.toArray(new JdbcDriverProperty[props.size()]);
    }

    protected JdbcDriverProperty[] getPropertyDescriptions( final DataSource dataSource,
                                                            final JdbcSource jdbcSource ) {
        final Method[] methods = dataSource.getClass().getMethods();
        final List props = new ArrayList();
        for (int i = 0; i < methods.length; i++) {
            final Method method = methods[i];
            final String methodName = method.getName();
            // If setter ...
            if (methodName.startsWith("set") && method.getParameterTypes().length == 1) { //$NON-NLS-1$
                // Get the property name
                final String name = methodName.substring(3); // remove the "set"
                final String desc = null;
                final String[] allowableValues = null;
                final boolean required = true;
                final JdbcDriverProperty prop = new JdbcDriverProperty(name, desc, allowableValues, required);
                props.add(prop);
            }
        }

        return (JdbcDriverProperty[])props.toArray(new JdbcDriverProperty[props.size()]);
    }

    public IStatus isValid( final JdbcSource jdbcSource ) {
        CoreArgCheck.isNotNull(jdbcSource);

        // A JdbcDriver is considered valid if all of the following conditions are true:
        // - There is a {@link JdbcSource#getName() name} that is not zero-length.
        final String name = jdbcSource.getName();
        if (name == null || name.trim().length() == 0) {
            final int code = NAME_NOT_SPECIFIED;
            return createError(code, JdbcPlugin.Util.getString("JdbcManagerImpl.The_name_is_empty")); //$NON-NLS-1$
        }
        if (!Character.isLetterOrDigit(name.charAt(0))) {
            final int code = NAME_MUST_BEGIN_WITH_LETTER_OR_NUMBER;
            return createError(code, JdbcPlugin.Util.getString("JdbcManagerImpl.The_name_must_begin_with_a_letter_or_digit")); //$NON-NLS-1$
        }

        // - There is a {@link JdbcSource#get preferred driver class}.
        final String driverClass = jdbcSource.getDriverClass();
        if (driverClass == null || driverClass.trim().length() == 0) {
            final int code = MISSING_DRIVER_CLASS;
            return createError(code,
                               JdbcPlugin.Util.getString("JdbcManagerImpl.A_driver_class_must_be_specified", jdbcSource.getName())); //$NON-NLS-1$
        }
        final IStatus error = checkClassNameForError(driverClass);
        if (error != null) {
            return error;
        }

        // - There are no null property names or values
        final Iterator iter = jdbcSource.getProperties().iterator();
        while (iter.hasNext()) {
            final JdbcSourceProperty prop = (JdbcSourceProperty)iter.next();
            final String propName = prop.getName();
            final String value = prop.getValue();
            if (propName == null || propName.trim().length() == 0) {
                final int code = PROPERTY_NAME_MISSING;
                return createError(code, JdbcPlugin.Util.getString("JdbcManagerImpl.Property_names_may_not_be_null_or_empty")); //$NON-NLS-1$
            }
            if (value == null || value.trim().length() == 0) {
                final int code = PROPERTY_VALUE_MISSING;
                return createError(code, JdbcPlugin.Util.getString("JdbcManagerImpl.Property_values_may_not_be_null_or_empty")); //$NON-NLS-1$
            }
        }

        IConnectionProfile profile = profileManager.getProfileByName(jdbcSource.getName());
        if (null == profile) {
            final int code = MISSING_CONNECTION_PROFILE;
            return createError(code,
                               JdbcPlugin.Util.getString("JdbcManagerImpl.Missing_connection_profile", jdbcSource.getName())); //$NON-NLS-1$
        } else if (!profile.arePropertiesComplete()) {
            final int code = INVALID_CONNECTION_PROFILE;
            return createError(code,
                               JdbcPlugin.Util.getString("JdbcManagerImpl.Invalid_connection_profile", jdbcSource.getName())); //$NON-NLS-1$	
        }
        final int code = VALID_SOURCE;
        return createOK(code, JdbcPlugin.Util.getString("JdbcManagerImpl.The_data_source_is_valid")); //$NON-NLS-1$
    }

    /* (non-Javadoc)
     * @see com.metamatrix.modeler.jdbc.JdbcManager#isValid(com.metamatrix.modeler.jdbc.JdbcDriver)
     */
    public IStatus isValid( final JdbcDriver driver ) {
        CoreArgCheck.isNotNull(driver);
        return isValid(driver, true);
    }

    /**
     * Helper method to actually do the checking of the JdbcDriver. This is also used by the {@link #getClassLoader(JdbcDriver)}
     * method, but that doesn't want to check driver class names.
     */
    protected IStatus isValid( final JdbcDriver driver,
                               final boolean checkDriverClasses ) {
        CoreArgCheck.isNotNull(driver);

        // A JdbcDriver is considered valid if all of the following conditions are true:
        // - There is a {@link JdbcDriver#getName() name} that is not zero-length.
        final String name = driver.getName();
        if (name == null || name.trim().length() == 0) {
            final int code = NAME_NOT_SPECIFIED;
            return createError(code, JdbcPlugin.Util.getString("JdbcManagerImpl.The_name_is_empty")); //$NON-NLS-1$
        }
        if (!Character.isLetterOrDigit(name.charAt(0))) {
            final int code = NAME_MUST_BEGIN_WITH_LETTER_OR_NUMBER;
            return createError(code, JdbcPlugin.Util.getString("JdbcManagerImpl.The_name_must_begin_with_a_letter_or_digit")); //$NON-NLS-1$
        }

        if (checkDriverClasses) {
            // - There is at least one {@link JdbcDriver#getAvailableDriverClassNames() driver class}.
            final List availableClasses = driver.getAvailableDriverClassNames();
            if (availableClasses.isEmpty()) {
                final int code = NO_AVAILABLE_DRIVER_CLASS_NAMES;
                return createError(code,
                                   JdbcPlugin.Util.getString("JdbcManagerImpl.No_java.sql.Driver_or_javax.sql.DataSource_classes_were_found", name)); //$NON-NLS-1$
            }
            final Iterator iter = availableClasses.iterator();
            while (iter.hasNext()) {
                final String className = (String)iter.next();
                final IStatus error = checkClassNameForError(className);
                if (error != null) {
                    return error;
                }
            }

            // - There is a {@link JdbcDriver#getPreferredDriverClassName() preferred driver class}.
            final String preferred = driver.getPreferredDriverClassName();
            if (preferred == null || preferred.trim().length() == 0) {
                final int code = PREFERRED_NOT_SPECIFIED;
                return createError(code,
                                   JdbcPlugin.Util.getString("JdbcManagerImpl.A_preferred_driver_class_must_be_chosen", name)); //$NON-NLS-1$
            }
            final IStatus error = checkClassNameForError(preferred);
            if (error != null) {
                return error;
            }

            // - There {@link JdbcDriver#getPreferredDriverClassNames() preferred driver class} is one of the
            // {@link JdbcDriver#getAvailableDriverClassNames() available driver classes}
            if (!availableClasses.contains(preferred)) {
                final int code = PREFERRED_NOT_IN_AVAILABLE;
                return createError(code,
                                   JdbcPlugin.Util.getString("JdbcManagerImpl.The_preferred_driver_class_is_not_available", name)); //$NON-NLS-1$
            }
        }

        // Additionally, a warning is included if any of the following are true:
        // - There are no {@link JdbcDriver#getJarFileUris() JAR file URIs}.
        if (driver.getJarFileUris().isEmpty()) {
            final int code = NO_JARS_SPECIFIED;
            return createWarning(code,
                                 JdbcPlugin.Util.getString("JdbcManagerImpl.There_are_no_JAR_files_specified.__Only_the_system_classpath_will_be_used", name)); //$NON-NLS-1$
        }
        final Iterator jarFileUriIter = driver.getJarFileUris().iterator();
        while (jarFileUriIter.hasNext()) {
            String jarFileUri = (String)jarFileUriIter.next();
            if (jarFileUri == null || jarFileUri.trim().length() == 0) {
                final int code = JAR_FILE_URI_NOT_SPECIFIED;
                return createError(code, JdbcPlugin.Util.getString("JdbcManagerImpl.The_jar_file_uri_is_empty", name)); //$NON-NLS-1$
            }
            // Try resolving the URL to an input stream ...
            boolean validUrl = false;
            InputStream stream = null;
            try {
                final URL url = new URL(jarFileUri);
                stream = url.openStream();
                validUrl = true;
            } catch (Throwable e) {
                // Let the rest of the patterns try ...
            } finally {
                if (stream != null) {
                    try {
                        stream.close();
                    } catch (IOException e1) {
                        // do nothing, because we don't care
                    }
                }
            }

            if (!validUrl) {
                // Try to resolve the URL to a file ...
                try {
                    final File jarFile = new File(jarFileUri);
                    if (!jarFile.exists()) {
                        final int code = JAR_FILE_DOESNT_EXIST;
                        final String msg = JdbcPlugin.Util.getString("JdbcManagerImpl.Supplied_path_does_not_exist", name, jarFileUri); //$NON-NLS-1$
                        return createError(code, msg);
                    }
                    jarFile.toURI().toURL();
                    validUrl = true;
                } catch (MalformedURLException e) {
                    // Let the rest of the patterns try ...
                }
            }
            if (!validUrl) {
                final int code = MALFORMED_URL;
                final String msg = JdbcPlugin.Util.getString("JdbcManagerImpl.Unable_to_create_class_loader_for_the_driver", name); //$NON-NLS-1$
                return createError(code, msg);
            }
        }

        // Check the URL syntax
        final String urlSyntax = driver.getUrlSyntax();
        if (urlSyntax != null && urlSyntax.trim().length() != 0) {
            // See if the URL syntax begin with "jdbc:"
            if (!urlSyntax.startsWith("jdbc:")) { //$NON-NLS-1$
                final int code = MALFORMED_URL_SYNTAX;
                final String msg = JdbcPlugin.Util.getString("JdbcManagerImpl.malformedUrlSyntax", name); //$NON-NLS-1$
                return createWarning(code, msg);
            }
        }

        final int code = VALID_DRIVER;
        return createOK(code, JdbcPlugin.Util.getString("JdbcManagerImpl.The_driver_specification_is_valid", name)); //$NON-NLS-1$
    }

    protected IStatus checkClassNameForError( final String className ) {
        if (className == null || className.trim().length() == 0) {
            final int code = NAME_NOT_SPECIFIED;
            return createError(code, JdbcPlugin.Util.getString("JdbcManagerImpl.The_class_name_is_empty")); //$NON-NLS-1$
        }
        if (!Character.isJavaIdentifierStart(className.charAt(0))) {
            final Object params = new Object[] {new Character(className.charAt(0))};
            final int code = ILLEGAL_CHAR_AT_START_OF_CLASS_NAME;
            return createError(code,
                               JdbcPlugin.Util.getString("JdbcManagerImpl.A_Java_class_may_not_begin_with_the_{0}_character", params)); //$NON-NLS-1$
        }
        CharacterIterator iter = new StringCharacterIterator(className);
        char c = iter.next(); // skip the first character
        while ((c = iter.next()) != CharacterIterator.DONE) {
            if (c != '.' && !Character.isJavaIdentifierPart(c)) {
                final Object params = new Object[] {new Character(className.charAt(0))};
                final int code = ILLEGAL_CHAR_IN_CLASS_NAME;
                return createError(code,
                                   JdbcPlugin.Util.getString("JdbcManagerImpl.A_Java_class_may_not_contain_the_{0}_character", params)); //$NON-NLS-1$
            }
        }
        return null;
    }

    protected IStatus createError( final int code,
                                   final String msg ) {
        return new Status(IStatus.ERROR, JdbcPlugin.PLUGIN_ID, code, msg, null);
    }

    protected IStatus createWarning( final int code,
                                     final String msg ) {
        return new Status(IStatus.WARNING, JdbcPlugin.PLUGIN_ID, code, msg, null);
    }

    protected IStatus createWarning( final int code,
                                     final Throwable t,
                                     final String msg ) {
        return new Status(IStatus.WARNING, JdbcPlugin.PLUGIN_ID, code, msg, t);
    }

    protected IStatus createOK( final int code,
                                String msg ) {
        return new Status(IStatus.OK, JdbcPlugin.PLUGIN_ID, code, msg, null);
    }

    /* (non-Javadoc)
     * @see com.metamatrix.modeler.jdbc.JdbcManager#findDrivers(java.lang.String)
     */
    public JdbcDriver[] findDrivers( String driverName ) {
        final List result = new ArrayList();
        final List instances = getJdbcDrivers();
        final Iterator iter = instances.iterator();
        while (iter.hasNext()) {
            final JdbcDriver instance = (JdbcDriver)iter.next();
            if (instance.getName().equalsIgnoreCase(driverName)) {
                result.add(instance);
            }
        }
        return (JdbcDriver[])result.toArray(new JdbcDriver[result.size()]);
    }

    /* (non-Javadoc)
     * @see com.metamatrix.modeler.jdbc.JdbcManager#findSources(java.lang.String)
     */
    public JdbcSource[] findSources( String sourceName ) {
        final List result = new ArrayList();
        final List instances = getJdbcSources();
        final Iterator iter = instances.iterator();
        while (iter.hasNext()) {
            final JdbcSource instance = (JdbcSource)iter.next();
            if (instance.getName().equalsIgnoreCase(sourceName)) {
                result.add(instance);
            }
        }
        return (JdbcSource[])result.toArray(new JdbcSource[result.size()]);
    }

	public class ProfileListener implements IProfileListener {

		/**
		 * {@inheritDoc}
		 * 
		 * @see org.eclipse.datatools.connectivity.IProfileListener#profileAdded(org.eclipse.datatools.connectivity.IConnectionProfile)
		 */
		@Override
		public void profileAdded(IConnectionProfile arg0) {
			try {
				reload(new NullProgressMonitor());
			} catch (JdbcException e) {
				// do nothing
			}
		}

		/**
		 * {@inheritDoc}
		 * 
		 * @see org.eclipse.datatools.connectivity.IProfileListener#profileChanged(org.eclipse.datatools.connectivity.IConnectionProfile)
		 */
		@Override
		public void profileChanged(IConnectionProfile arg0) {
			try {
				reload(new NullProgressMonitor());
			} catch (JdbcException e) {
				// do nothing
			}
		}

		/**
		 * {@inheritDoc}
		 * 
		 * @see org.eclipse.datatools.connectivity.IProfileListener#profileDeleted(org.eclipse.datatools.connectivity.IConnectionProfile)
		 */
		@Override
		public void profileDeleted(IConnectionProfile arg0) {
			try {
				reload(new NullProgressMonitor());
			} catch (JdbcException e) {
				// do nothing
			}
		}

	}
}
