% PAResult/PAisAwaited checks if results among an array of PAResult objects are
% available or not
%
% Syntax
%       >> tf=PAisAwaited(r)
% 
% Inputs
%
%   r - an array of PAResult objects received by a call to PAsolve
%
% Outputs
%   
%   tf - an array of boolean values, telling for each indice if the result is awaited (true) or available(false).
%
% Example
%
%   >> r=PAsolve(@factorial, 1, 2, 3, 4);
%   >> tf = PAisAwaited(r)  % Non-blocking call
%
% See Also
%   PAsolve, PAResult/PAwaitAny, PAResult/PAwaitFor
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
function A = PAisAwaited(this)
sched = PAScheduler;
% Get the solver from memory
solver = sched.PAgetsolver();
s=size(this);
R=this(1,1);
jobid = R.jobid;
taskids = java.util.ArrayList(s(1)*s(2));
for i=1:s(1)
    for j=1:s(2)        
        R=this(i,j);
        taskids.add(R.taskid);                
    end
end
try
    unrei = solver.areAwaited(jobid, taskids);
catch
     PAensureConnected();
     unrei = solver.areAwaited(jobid, taskids);
end
answers = unrei.get();
for i=1:s(1)
    for j=1:s(2)        
        A(i,j)=answers.get((i-1)*s(2)+(j-1));        
    end
end

