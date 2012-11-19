% PAbeginSession starts a fault-tolerant PAsolve session.
%
% Syntax
%
%       >> PAbeginSession();
%
% Description
%
%       By calling PAbeginSession, we start a new fault-tolerant session. Multiple session cannot be started at the same time,
%       (i.e. only one session can be active at a time).
%       A fault-tolerant session means that every PAsolve call will be recorded and results received via PAwaitFor or PAwaitAny
%       will be remembered. If Matlab crashes in the middle of a session (or simply exits by the user's will), it will be possible
%       to restart the distributed computation at the point it was before the crash. In order to do that, simply call PAbeginSession()
%       after Matlab restart and resubmit the same computations. PAsolve calls which have already been submitted will not
%       be submitted again to the scheduler and will be linked to the previously submitted job. PAwaitFor calls which have
%       already completed in the previous session will return immediately with the correct results.
%       Function code, Parameters, Input/Output Files used during the previous session should NOT be changed (i.e. changes
%       will be ignored).
%       Multiple matlab crash/exits can occur in a row, simply reuse PAbeginSession() at each restart.
%       In order to finish a fault-tolerant session, PAendSession() must be called.
%
% Example
%       >> PAbeginSession()
%       >> res1 = PAsolve(@longComputation1, param_1_1, param_1_2, param_1_3)
%       >> val1 = PAwaitFor(res1);
%       (...)
%       >> res1 = PAsolve(@longComputation2, param_2_1, param_2_2, param_2_3)
%       >> xxxx CRASH xxxxx
%       (... next matlab session ...)
%       >> PAbeginSession()
%       >> res1 = PAsolve(@longComputation1, param_1_1, param_1_2, param_1_3)
%       >> val1 = PAwaitFor(res1);  % Instantaneous !
%       >> res2 = PAsolve(@longComputation2, param_2_1, param_2_2, param_2_3)
%       >> val2 = PAwaitFor(res2);
%       (... etc ...)
%       >> PAendSession();
%
% See also
%       PAendSession, PAgetResults, PAsolve, PAResult/PAwaitFor, PAResult/PAwaitAny

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
function [tf] = PAbeginSession()
    PAensureConnected();
    repository = org.ow2.proactive.scheduler.ext.matlab.client.embedded.MatlabTaskRepository.getInstance();
    repository.beginSession();
    sched = PAScheduler();
    solver = sched.PAgetsolver();
    pair = solver.beginSession();
    tf = pair.getX();
    message = pair.getY();
    fprintf('%s\n',message);
end