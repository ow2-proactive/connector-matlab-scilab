% PAgetResults results from a job
%
% Syntax
%
%       >> res = PAgetResults(jobid);
%
% Inputs
%       
%       jobid - the id of the job (string or numeric)
%
% Ouputs
%
%       res - an array of PAresult objects
% 
% Description
%
%       PAgetResults is used to retrieve results of a PAsolve call from a
%       previous Matlab session (Disconnected mode).
%       The array of objects returned is the same returned by
%       the initial call to PAsolve during the previous session. Calls to
%       PAwaitFor or PAwaitAny can be done on this array if some results
%       are not available yet.
%
% Example
%        >> res = PAgetResults('1')
%
% See also
%
%       PAsolve, PAResult/PAwaitFor, PAResult/PAwaitAny

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
function res = PAgetResults(jobid)
if ~ischar(jobid) && ~isnumeric(jobid)
    error('PAgetResults::JobId must be a string or a number');
end
if isnumeric(jobid)
    jobid = num2str(jobid);
end
sched = PAScheduler;
solver = sched.PAgetsolver();
if strcmp(class(solver),'double')
    error('connexion to the scheduler is not established');
end
trep = org.ow2.proactive.scheduler.ext.matlab.client.embedded.MatlabTaskRepository.getInstance();

jinfo = trep.getInfo(jobid);

disp(['Retrieving results of job ' jobid]);

ftn = jinfo.getFinalTasksNamesAsList();
dir_to_clean = char(jinfo.getDirToClean();
tnit = ftn.iterator();
for i=1:ftn.size()
    taskinfo.cleanDir = dir_to_clean;
    out_path = jinfo.getOutputVariablePathWithIndex(i-1);
    taskinfo.outFile = char(out_path);
    taskinfo.jobid = jobid;
    taskinfo.taskid = char(tnit.next());
    res(i)=PAResult(taskinfo);
end
