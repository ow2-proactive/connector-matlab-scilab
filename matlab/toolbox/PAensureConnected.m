function PAensureConnected()
% PAensureConnected makes sure that we are connected to the ProActive Scheduler.
%
% Syntax
%
%       PAensureConnected();
%
%
%
% Description
%
%       PAensureConnected makes sure that we are connected to the ProActive Scheduler. If we are not connected,
%       PAensureConnected will try to reconnect and will keep trying until the scheduler is reachable again. It will use
%       the same credentials provided during the PAconnect call.
%
%
% See also
%   PAconnect, PAdisconnect, PAisConnected
%

% /*
%   * ################################################################
%   *
%   * ProActive Parallel Suite(TM): The Java(TM) library for
%   *    Parallel, Distributed, Multi-Core Computing for
%   *    Enterprise Grids & Clouds
%   *
%   * Copyright (C) 1997-2011 INRIA/University of
%   *                 Nice-Sophia Antipolis/ActiveEon
%   * Contact: proactive@ow2.org or contact@activeeon.com
%   *
%   * This library is free software; you can redistribute it and/or
%   * modify it under the terms of the GNU Affero General Public License
%   * as published by the Free Software Foundation; version 3 of
%   * the License.
%   *
%   * This library is distributed in the hope that it will be useful,
%   * but WITHOUT ANY WARRANTY; without even the implied warranty of
%   * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
%   * Affero General Public License for more details.
%   *
%   * You should have received a copy of the GNU Affero General Public License
%   * along with this library; if not, write to the Free Software
%   * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307
%   * USA
%   *
%   * If needed, contact us to obtain a release under GPL Version 2 or 3
%   * or a different license than the AGPL.
%   *
%   *  Initial developer(s):               The ProActive Team
%   *                        http://proactive.inria.fr/team_members.htm
%   *  Contributor(s):
%   *
%   * ################################################################
%   * $$PROACTIVE_INITIAL_DEV$$
%   */
function PAensureConnected()
     sched = PAScheduler;
     solver = sched.PAgetsolver();
     if strcmp(class(solver),'double')
         error('Initial connection to the scheduler was not established, use PAconnect');
     end
     try
        solver.ensureConnection();
     catch
        % if there is an exception it means that we lost the RMI stubs
        deployer = org.ow2.proactive.scheduler.ext.matlab.client.embedded.util.MatlabJVMSpawnHelper.getInstance();
        deployer.updateAllStubs(true);
        solver = deployer.getMatlabEnvironment();
        sched.PAgetsolver(solver);
        registry = deployer.getDSRegistry();
        sched.PAgetDataspaceRegistry(registry);
        jvmint = deployer.getJvmInterface();
        sched.PAgetJVMInterface(jvmint);
        pause(2);
        PAensureConnected();

     end

end