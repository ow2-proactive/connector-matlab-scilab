/*
 * ################################################################
 *
 * ProActive Parallel Suite(TM): The Java(TM) library for
 *    Parallel, Distributed, Multi-Core Computing for
 *    Enterprise Grids & Clouds
 *
 * Copyright (C) 1997-2012 INRIA/University of
 *                 Nice-Sophia Antipolis/ActiveEon
 * Contact: proactive@ow2.org or contact@activeeon.com
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; version 3 of
 * the License.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307
 * USA
 *
 * If needed, contact us to obtain a release under GPL Version 2 or 3
 * or a different license than the AGPL.
 *
 *  Initial developer(s):               The ProActive Team
 *                        http://proactive.inria.fr/team_members.htm
 *  Contributor(s):
 *
 * ################################################################
 * $$PROACTIVE_INITIAL_DEV$$
 */
package org.ow2.proactive.scheduler.ext.matsci.middleman;

import org.objectweb.proactive.api.PAActiveObject;
import org.objectweb.proactive.core.body.BodyMap;
import org.objectweb.proactive.core.body.LocalBodyStore;
import org.objectweb.proactive.core.node.Node;
import org.objectweb.proactive.core.node.NodeFactory;
import org.objectweb.proactive.core.remoteobject.AbstractRemoteObjectFactory;
import org.objectweb.proactive.core.runtime.ProActiveRuntimeImpl;
import org.objectweb.proactive.extensions.pamr.client.AgentImpl;
import org.objectweb.proactive.extensions.pamr.client.Tunnel;
import org.objectweb.proactive.extensions.pamr.exceptions.PAMRException;
import org.objectweb.proactive.extensions.pamr.protocol.AgentID;
import org.objectweb.proactive.extensions.pamr.protocol.MagicCookie;
import org.objectweb.proactive.extensions.pamr.remoteobject.PAMRRemoteObjectFactory;
import org.objectweb.proactive.extensions.pamr.remoteobject.util.socketfactory.PAMRSocketFactorySPI;
import org.objectweb.proactive.utils.NamedThreadFactory;
import org.ow2.proactive.scheduler.ext.matsci.client.common.DataspaceRegistry;
import org.ow2.proactive.scheduler.ext.matsci.client.common.MatSciEnvironment;
import org.ow2.proactive.scheduler.ext.matsci.client.common.MatSciJVMProcessInterface;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.Socket;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Hashtable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;


/**
 * MiddlemanDeployer a main class used to deploy the environment and dataspace dsregistry in the middleman JVM
 *
 * @author The ProActive Team
 */
public abstract class MiddlemanDeployer {

    protected static MiddlemanDeployer instance;

    /**
     * Standalone objects
     */
    private AODataspaceRegistry reg;
    private MatSciJVMProcessInterfaceImpl itf;

    /**
     * ProActive Stubs
     */
    private AODataspaceRegistry pastub_reg;
    private MatSciJVMProcessInterfaceImpl pastub_itf;

    /**
     * RMI stubs
     */
    private DataspaceRegistry rmistub_reg;
    private MatSciJVMProcessInterface rmistub_itf;

    protected Registry registry;

    protected boolean debug;

    protected int port;

    protected long old_agent_id = -1;

    protected long new_agent_id;

    protected static final String MIDDLEMAN_NODE_NAME = "MiddlemanNode";

    protected Node mainNode;

    private ThreadFactory tf = new NamedThreadFactory("Middleman Thread Factory");

    final protected ExecutorService tpe = Executors.newFixedThreadPool(1, tf);

    protected MiddlemanDeployer() {

    }

    public static MiddlemanDeployer getInstance() {
        if (instance == null) {
            throw new IllegalStateException("instance should have already been initialized by subclass");
        }
        return instance;
    }

    /**
     * This hook monitors both a network connection problem with the PAMR router and a router restart. In the first case,
     * it will simply reconnect to the router after the network is reachable again, in the second case, it will restart everything
     * on this JVM and reinitialize the connection to the new router
     */
    protected void PAMRHook() {
        try {
            PAMRRemoteObjectFactory f = (PAMRRemoteObjectFactory) AbstractRemoteObjectFactory
                    .getRemoteObjectFactory("pamr");
            final AgentImpl agent = (AgentImpl) f.getAgent();

            if (agent == null) {
                return;
            }

            final Field privateField = AgentImpl.class.getDeclaredField("socketFactory");
            privateField.setAccessible(true);

            final Field routerIdField = AgentImpl.class.getDeclaredField("routerID");
            routerIdField.setAccessible(true);

            final Field agentIdField = AgentImpl.class.getDeclaredField("agentID");
            agentIdField.setAccessible(true);

            final Field requestIDGeneratorField = AgentImpl.class.getDeclaredField("requestIDGenerator");
            Class rHEClz = null;
            Class wrClz = null;
            Class<?>[] class_array = AgentImpl.class.getDeclaredClasses();
            for (Class clz : class_array) {
                if (clz.getName().endsWith("RouterHandshakeException")) {
                    rHEClz = clz;
                } else if (clz.getName().endsWith("WaitingRoom")) {
                    wrClz = clz;
                }
            }
            final Class routerHandshakeExceptionClass = rHEClz;
            final Class waitingRoomClass = wrClz;
            final Field byRemoteAgentField = waitingRoomClass.getDeclaredField("byRemoteAgent");
            byRemoteAgentField.setAccessible(true);
            final Method unlockDueToTunnelFailureMethod = waitingRoomClass.getDeclaredMethod(
                    "unlockDueToTunnelFailure", PAMRException.class);
            unlockDueToTunnelFailureMethod.setAccessible(true);

            requestIDGeneratorField.setAccessible(true);

            final Field magicCookieField = AgentImpl.class.getDeclaredField("magicCookie");
            magicCookieField.setAccessible(true);

            final Field tunnelField = AgentImpl.class.getDeclaredField("t");
            tunnelField.setAccessible(true);

            final Field mailboxesField = AgentImpl.class.getDeclaredField("mailboxes");
            mailboxesField.setAccessible(true);

            final PAMRSocketFactorySPI target = (PAMRSocketFactorySPI) privateField.get(agent);
            privateField.set(agent, new PAMRSocketFactorySPI() {

                private boolean initialValueSet = false;
                private Long oldRouterId;
                private AgentID oldAgentId;
                private MagicCookie oldMagicCookie;
                private AtomicLong oldRequestIdGenerator;
                private boolean refusedHandshake = false;

                @Override
                public Socket createSocket(String host, int port) throws IOException {

                    final Socket targetSocket = target.createSocket(host, port);
                    try {
                        if (!initialValueSet) {
                            // We store the initial values of ids in case of simple network failure and not router restart
                            oldRouterId = (Long) routerIdField.get(agent);
                            oldAgentId = (AgentID) agentIdField.get(agent);
                            old_agent_id = oldAgentId.getId();
                            oldMagicCookie = (MagicCookie) magicCookieField.get(agent);
                            oldRequestIdGenerator = (AtomicLong) requestIDGeneratorField.get(agent);
                            initialValueSet = true;
                        }
                        // Those values reinitialize the connection
                        if (refusedHandshake) {
                            // if there was a refusedHandshake we roll back to initial values
                            routerIdField.set(agent, oldRouterId);
                            agentIdField.set(agent, oldAgentId);
                            requestIDGeneratorField.set(agent, oldRequestIdGenerator);
                            magicCookieField.set(agent, oldMagicCookie);
                        } else {
                            routerIdField.set(agent, Long.MIN_VALUE);
                            agentIdField.set(agent, null);
                            requestIDGeneratorField.set(agent, new AtomicLong(0));
                            magicCookieField.set(agent, new MagicCookie());
                        }

                    } catch (Exception eee) {
                        System.out.println("[MiddlemanDeployer] Unexpected exception : ");
                        eee.printStackTrace();
                    }

                    Tunnel tunnel = new Tunnel(targetSocket);
                    try {
                        // Initiate a manual handhsake
                        Method routerHandshake = agent.getClass().getDeclaredMethod("routerHandshake",
                                Tunnel.class);
                        System.out
                                .println("[MiddlemanDeployer] Trying to reconnect, reestablishing a tunnel...");
                        routerHandshake.setAccessible(true);
                        routerHandshake.invoke(agent, tunnel);
                        // This sets a new valid tunnel
                        tunnelField.set(agent, tunnel);
                        Object mailboxes = mailboxesField.get(agent);
                        unlockDueToTunnelFailureMethod.invoke(mailboxes, new PAMRException("Router Reset"));
                        //Map byRemoteAgent = (Map) byRemoteAgentField.get(mailboxes);
                        //byRemoteAgent.clear();
                        // we reinitialize the initialValues as well (a new tunnel will mean new values)
                        initialValueSet = false;
                        System.out.println("[MiddlemanDeployer] Reconnected with PAMR router with new ID : " +
                            agent.getAgentID());
                        System.out
                                .println("A lot of error messages will be displayed, this is due to the reconnection process.");
                        new_agent_id = agent.getAgentID().getId();
                        // We rebind all our active objects
                        tpe.submit(new RestartRunnable());
                    } catch (Throwable e) {
                        if (e.getClass().isInstance(routerHandshakeExceptionClass)) {
                            // if there was a router handshake exception, it means that the router was lost but still alive (i.e. network failure).
                            // we thus reinitialize the ids to the initial values
                            refusedHandshake = true;
                        } else {
                            refusedHandshake = false;
                        }
                        // Exception occurred, we close the tunnel and throw an exception
                        // the agent will try to reconnect
                        try {
                            tunnel.shutdown();
                        } catch (Exception ee) {
                        }
                        throw new IOException(e);
                    }
                    throw new IOException("dummy exception to skip legacy reconnect");
                }

                @Override
                public String getAlias() {
                    return target.getAlias();
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    protected void submitMain() {
        tpe.submit(new MainRunnable());
    }

    /**
     * Initializes the dataspace registry and JVM interface
     * @param env
     * @throws Exception
     */
    protected void init(MatSciEnvironment env) throws Exception {
        reg = new AODataspaceRegistry();
        itf = new MatSciJVMProcessInterfaceImpl(env);

        registry = LocateRegistry.createRegistry(port);
    }

    /**
     * Starts the RMI and ProActive objects
     * @throws Exception
     */
    protected void start() throws Exception {
        System.out.println("[MiddlemanDeployer] Starting Middleman JVM");
        activate();
        exportAll();
        rebindAll();
        System.out.println("[MiddlemanDeployer] Middleman JVM started");
    }

    /**
     * Restart the RMI and ProActive objects
     * @throws Exception
     */
    protected void restart() throws Exception {
        System.out.println("[MiddlemanDeployer] Cleaning Middleman JVM");
        unbindAll();
        unexportAll();
        System.out.println("[MiddlemanDeployer] RMI stubs unexported");
        terminateAO();
        System.out.println("[MiddlemanDeployer] AOs Terminated");
        cleanContext();
        start();
    }

    /**
     * Public interface to restart, this method is asynchronous
     */
    public void restartAll() {
        // We rebind all our active objects
        tpe.submit(new RestartRunnable());
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public boolean isDebug() {
        return debug;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    protected abstract void init() throws Exception;

    protected class MainRunnable implements Runnable {

        public MainRunnable() {

        }

        @Override
        public void run() {
            PAMRHook();
            try {
                init();
            } catch (Throwable e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
            try {
                start();
            } catch (Throwable e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        }
    }

    protected class RestartRunnable implements Runnable {

        public RestartRunnable() {

        }

        @Override
        public void run() {
            try {
                restart();
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        }
    }

    protected void activate() throws Exception {
        try {
            mainNode = NodeFactory.createLocalNode(MIDDLEMAN_NODE_NAME, true, null, null);
        } catch (Exception e) {
            e.printStackTrace();
            // try again
            mainNode = NodeFactory.createLocalNode(MIDDLEMAN_NODE_NAME, true, null, null);
        }
        pastub_reg = PAActiveObject.turnActive(reg, mainNode);
        pastub_itf = PAActiveObject.turnActive(itf, mainNode);
    }

    protected void cleanContext() {
        // This removes all half bodies
        try {
            LocalBodyStore.getInstance().clearAllContexts();
            BodyMap bm = LocalBodyStore.getInstance().getLocalHalfBodies();

            Field privateField = bm.getClass().getDeclaredField("idToBodyMap");
            privateField.setAccessible(true);
            Hashtable ht = (Hashtable) privateField.get(bm);
            ht.clear();

            ProActiveRuntimeImpl paruntime = ProActiveRuntimeImpl.getProActiveRuntime();
            //            paruntime.killNode(MIDDLEMAN_NODE_NAME);
            //
            //            RemoteObjectExposer roe = paruntime.getRemoteObjectExposer();
            //
            //            String old_runtimeUrl = roe.getURL("pamr");
            //            String new_runtimeUrl = old_runtimeUrl.replace("://"+old_agent_id,"://"+new_agent_id);
            //
            //            Field activeRemoteRemoteObjectsField = roe.getClass().getDeclaredField("activeRemoteRemoteObjects");
            //            activeRemoteRemoteObjectsField.setAccessible(true);
            //
            //            LinkedHashMap<URI, InternalRemoteRemoteObject> activeRemoteRemoteObjects = (LinkedHashMap<URI, InternalRemoteRemoteObject>) activeRemoteRemoteObjectsField.get(roe);
            //
            //            InternalRemoteRemoteObject pamrRuntime = activeRemoteRemoteObjects.get(new URI(old_runtimeUrl));
            //            activeRemoteRemoteObjects.remove(new URI(old_runtimeUrl));
            //
            //            activeRemoteRemoteObjects.put(new URI(new_runtimeUrl), pamrRuntime);
            paruntime.cleanJvmFromPA();
            // Restart Runtime
            paruntime = ProActiveRuntimeImpl.getProActiveRuntime();

            //            Iterator<UniversalBody> it = LocalBodyStore.getInstance().getLocalBodies().bodiesIterator();
            //            while (it.hasNext()) {
            //                Body body = (Body) it.next();
            //                body.getRequestQueue().destroy();
            //                while (!body.isActive()) {
            //                    Thread.sleep(5);
            //                }
            //            }

        } catch (Exception ex) {
            System.out.println("Main.main() unable to clean half bodies" + ex.getMessage());
        }
    }

    protected void terminateAO() throws Exception {
        reg.terminateFast();
        itf.terminateFast();
    }

    protected void unbindAll() throws Exception {
        registry.unbind(DataspaceRegistry.class.getName());
        registry.unbind(MatSciJVMProcessInterface.class.getName());
    }

    protected void unexportAll() throws Exception {
        UnicastRemoteObject.unexportObject(pastub_reg, true);
        UnicastRemoteObject.unexportObject(pastub_itf, true);
    }

    protected void exportAll() throws Exception {
        rmistub_reg = (DataspaceRegistry) UnicastRemoteObject.exportObject(pastub_reg);
        rmistub_itf = (MatSciJVMProcessInterface) UnicastRemoteObject.exportObject(pastub_itf);
    }

    protected void rebindAll() throws Exception {
        registry.rebind(DataspaceRegistry.class.getName(), rmistub_reg);
        registry.rebind(MatSciJVMProcessInterface.class.getName(), rmistub_itf);
    }
}
