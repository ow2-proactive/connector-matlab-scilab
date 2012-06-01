package functionaltests;

import org.ow2.proactive.authentication.crypto.CredData;
import org.ow2.proactive.authentication.crypto.Credentials;
import org.ow2.proactive.resourcemanager.RMFactory;
import org.ow2.proactive.resourcemanager.authentication.RMAuthentication;
import org.ow2.proactive.resourcemanager.core.properties.PAResourceManagerProperties;
import org.ow2.proactive.resourcemanager.frontend.RMConnection;
import org.ow2.proactive.resourcemanager.frontend.ResourceManager;
import org.ow2.proactive.resourcemanager.nodesource.NodeSource;
import org.ow2.proactive.resourcemanager.nodesource.infrastructure.LocalInfrastructure;
import org.ow2.proactive.resourcemanager.nodesource.policy.StaticPolicy;
import org.ow2.proactive.utils.FileToBytesConverter;
import org.ow2.tests.ProActiveSetup2;

import java.io.File;


/**
 *  Helper to start the RM
 *
 * @author ProActive team
 * @since ProActive 5.2.0
 */
public class RMTHelper {

    final protected static ProActiveSetup2 setup = new ProActiveSetup2();

    /**
     * Number of nodes deployed with default deployment descriptor
     */
    public static int defaultNodesNumber = 4;

    protected ResourceManager resourceManager;

    private static RMTHelper defaultInstance = new RMTHelper();

    protected RMAuthentication auth;
    /**
     * Timeout for local infrastructure
     */
    public static final int defaultNodesTimeout = 20 * 1000; //20s

    /**
     * Default user name for RM's connection
     */
    public static String username = "demo";

    /**
     * Default password for RM's connection
     */
    public static String password = "demo";

    public static RMTHelper getDefaultInstance() {
        return defaultInstance;
    }

    /**
     * Idem than connect but allows to specify a propertyFile used to start the RM
     */
    public ResourceManager connect(String name, String pass) throws Exception {
        RMAuthentication authInt = getRMAuth();
        Credentials cred = Credentials.createCredentials(new CredData(CredData.parseLogin(name), CredData
                .parseDomain(name), pass), authInt.getPublicKey());

        return authInt.login(cred);
    }

    /**
     * Same as getRMAuth but allows to specify a property file used to start the RM
     *
     * @return
     * @throws Exception
     */
    public RMAuthentication getRMAuth() throws Exception {
        if (auth == null) {
            // waiting the initialization
            auth = RMConnection.waitAndJoin(null);
        }
        return auth;
    }

    /**
     * Gets the connected ResourceManager interface.
     */
    public ResourceManager getResourceManager() throws Exception {
        if (resourceManager == null) {
            resourceManager = connect(username, password);
        }
        return resourceManager;
    }

    /**
     * Creates a Local Infrastructure Manager with defaultNodesNumber nodes
     *
     * @throws Exception
     */
    public void createLocalNodeSource() throws Exception {
        RMFactory.setOsJavaProperty();
        ResourceManager rm = getResourceManager();
        System.err.println(setup.getJvmParameters());
        //first emtpy im parameter is default rm url
        byte[] creds = FileToBytesConverter.convertFileToByteArray(new File(PAResourceManagerProperties
                .getAbsolutePath(PAResourceManagerProperties.RM_CREDS.getValueAsString())));
        rm.createNodeSource(NodeSource.LOCAL_INFRASTRUCTURE_NAME, LocalInfrastructure.class.getName(),
                new Object[] { "", creds, RMTHelper.defaultNodesNumber, RMTHelper.defaultNodesTimeout,
                        setup.getJvmParameters() }, StaticPolicy.class.getName(), null);
    }

}
