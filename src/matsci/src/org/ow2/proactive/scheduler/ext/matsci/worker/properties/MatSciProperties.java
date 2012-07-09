package org.ow2.proactive.scheduler.ext.matsci.worker.properties;

import org.objectweb.proactive.core.runtime.ProActiveRuntimeImpl;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;


/**
 * MatSciProperties contains all Matlab/Scilab connector properties.
 *
 * You must use provided methods in order to get the MatSci properties.
 *
 * @author The ProActiveTeam
 * @since ProActive 4.0
 *
 * $Id$
 */
public enum MatSciProperties {

    /** Scheduler home directory */
    SCHEDULER_HOME("pa.scheduler.home", PropertyType.STRING),

    /** Tells where to find matlab worker configuration file */
    MATLAB_WORKER_CONFIGURATION_FILE("pa.matlab.config.worker", PropertyType.STRING),
    /** Tells where to find matlab worker configuration file */
    SCILAB_WORKER_CONFIGURATION_FILE("pa.scilab.config.worker", PropertyType.STRING);

    /* ***************************************************************************** */
    /* ***************************************************************************** */
    public static final String MATSCI_PROPERTIES_FILEPATH = "pa.matsci.properties.filepath";
    /** Default properties file for the scheduler configuration */
    private static final String DEFAULT_PROPERTIES_FILE = "addons/matlab_scilab_connector.ini";

    private static String properties_file = null;
    /** to know if the file has been loaded or not */
    private static boolean fileLoaded;
    /** memory entity of the properties file. */
    private static Properties prop = null;

    /** Key of the specific instance. */
    private String key;
    /** value of the specific instance. */
    private PropertyType type;

    private static File schedHome = null;

    /**
     * Create a new instance of PASchedulerProperties
     *
     * @param str the key of the instance.
     * @param type the real java type of this instance.
     */
    MatSciProperties(String str, PropertyType type) {
        this.key = str;
        this.type = type;
    }

    /**
     * Get the key.
     *
     * @return the key.
     */
    public String getKey() {
        return key;
    }

    /**
     * Set the value of this property to the given one.
     *
     * @param value the new value to set.
     */
    public void updateProperty(String value) {
        getProperties();
        prop.setProperty(key, value);
    }

    /**
     * Set the user java properties to the MatSciProperties.<br/>
     * User properties are defined using the -Dname=value in the java command.
     */
    private static void setUserJavaProperties() {
        if (prop != null) {
            for (Object o : prop.keySet()) {
                String s = System.getProperty((String) o);
                if (s != null) {
                    prop.setProperty((String) o, s);
                }
            }
        }
    }

    /**
     * Initialize the file to be loaded by this properties.
     * It first check the filename argument :<br>
     * - if null  : default config file is used (first check if java property file exist)<br>
     * - if exist : use the filename argument to read configuration.<br>
     *
     * Finally, if the selected file is a relative path, the file will be relative to the SCHEDULER_HOME property.
     *
     */
    private static void init() {
        String propertiesPath;
        boolean jPropSet = false;
        if (System.getProperty(MATSCI_PROPERTIES_FILEPATH) != null) {
            propertiesPath = System.getProperty(MATSCI_PROPERTIES_FILEPATH);
            jPropSet = true;
        } else {
            propertiesPath = DEFAULT_PROPERTIES_FILE;
        }

        if (!new File(propertiesPath).isAbsolute()) {
            File schedhome = null;
            try {
                schedhome = findSchedulerHome();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            propertiesPath = schedhome + File.separator + propertiesPath;
        }
        properties_file = propertiesPath;
        System.out.println("Using properties file at :" + properties_file);
        fileLoaded = new File(properties_file).exists();
        if (jPropSet && !fileLoaded) {
            throw new RuntimeException("Matlab/Scilab properties file not found : '" + properties_file + "'");
        }
    }

    /**
     * Get the properties map or load it if needed.
     *
     * @return the properties map corresponding to the default property file.
     */
    private static Properties getProperties() {
        if (prop == null) {
            prop = new Properties();
            init();

            if (!fileLoaded) {
                return prop;
            }
            try {

                prop.load(new FileInputStream(properties_file));
                setUserJavaProperties();

            } catch (IOException e) {
                throw new RuntimeException(
                    "Error when loading properties from Matlab/Scilab properties file at : '" +
                        properties_file + "'", e);
            }
        }
        return prop;
    }

    /**
     * Return true if this property is set, false otherwise.
     *
     * @return true if this property is set, false otherwise.
     */
    public boolean isSet() {
        getProperties();
        if (fileLoaded) {
            return prop.containsKey(key);
        } else {
            return false;
        }
    }

    /**
     * Returns the string to be passed on the command line
     *
     * The property surrounded by '-D' and '='
     *
     * @return the string to be passed on the command line
     */
    public String getCmdLine() {
        return "-D" + key + '=';
    }

    /**
     * Returns the value of this property as an integer.
     * If value is not an integer, an exception will be thrown.
     *
     * @return the value of this property.
     */
    public int getValueAsInt() {
        getProperties();
        if (fileLoaded) {
            String valueS = getValueAsString();
            try {
                int value = Integer.parseInt(valueS);
                return value;
            } catch (NumberFormatException e) {
                RuntimeException re = new IllegalArgumentException(key +
                    " is not an integer property. getValueAsInt cannot be called on this property");
                throw re;
            }
        } else {
            return 0;
        }
    }

    /**
     * Returns the value of this property as a string.
     *
     * @return the value of this property.
     */
    public String getValueAsString() {
        Properties prop = getProperties();
        if (fileLoaded) {
            return prop.getProperty(key);
        } else {
            return "";
        }
    }

    /**
     * Returns the value of this property as a boolean.
     * If value is not a boolean, an exception will be thrown.<br>
     * The behavior of this method is the same as the {@link java.lang.Boolean#parseBoolean(String s)}.
     *
     * @return the value of this property.
     */
    public boolean getValueAsBoolean() {
        getProperties();
        if (fileLoaded) {
            return Boolean.parseBoolean(getValueAsString());
        } else {
            return false;
        }
    }

    /**
     * Return the type of the given properties.
     *
     * @return the type of the given properties.
     */
    public PropertyType getType() {
        return type;
    }

    /**
     * @see java.lang.Enum#toString()
     */
    @Override
    public String toString() {
        return getValueAsString();
    }

    /**
     * Get the absolute path of the given path.<br/>
     * It the path is absolute, then it is returned. If the path is relative, then the Scheduler_home directory is
     * concatenated in front of the given string.
     *
     * @param userPath the path to check transform.
     * @return the absolute path of the given path.
     */
    public static String getAbsolutePath(String userPath) {
        if (new File(userPath).isAbsolute()) {
            return userPath;
        } else {
            return MatSciProperties.SCHEDULER_HOME.getValueAsString() + File.separator + userPath;
        }
    }

    /**
     * Supported types for PASchedulerProperties
     */
    public enum PropertyType {
        STRING, BOOLEAN, INTEGER;
    }

    public static File findSchedulerHome() throws Exception {
        if (schedHome == null) {
            String homestr = null;
            try {
                homestr = ProActiveRuntimeImpl.getProActiveRuntime().getProActiveHome();
            } catch (Exception e) {
                // Try to locate dynamically for the location of the current class file
                final String path = MatSciProperties.class.getProtectionDomain().getCodeSource()
                        .getLocation().getPath();
                final File f = new File(path);
                File schedulerHome = null;

                // If the path contains 'classes' the scheduler home 2 parent dirs
                if (path.contains("classes")) {
                    schedulerHome = f.getParentFile().getParentFile();
                } else { // means its in dist
                    schedulerHome = f.getParentFile();
                }

                homestr = schedulerHome.getAbsolutePath();

                // Unable to locate dynamically the location of the scheduler home throw
                // exception to inform about the initial problem
                if (!(new File(homestr)).exists()) {
                    throw e;
                }
            }
            schedHome = new File(homestr);
        }
        return schedHome;
    }
}
