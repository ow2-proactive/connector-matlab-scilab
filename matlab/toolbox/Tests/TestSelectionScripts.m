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
function [ok, msg]=TestSelectionScripts(nbiter,timeout)
if ~exist('timeout', 'var')
    if ispc()
        timeout = 500000;
    else
        timeout = 200000;
    end
end
opt = PAoptions();
if ~exist('nbiter', 'var')
    nbiter = 1;
end
for kk=1:nbiter
    disp('-------------------------------------');
    disp(['------------------------Iteration '  num2str(kk)]);
    disp('...... Testing task selection script');
    % Testing task selection script
    tsk = PATask(1,2);
    tsk(1,1:2).Func = @factorial;
    tsk(1,1).SelectionScript = fullfile(opt.MatSciDir, 'Tests','script','truescript.js');
    tsk(1,1).Static = true;
    tsk(1,2).SelectionScript = fullfile(opt.MatSciDir, 'Tests', 'script','scriptwithparam.js');
    xx = clock();
    sd='';
    for i=1:length(xx)
        sd=[sd num2str(xx(i)) '_'];
    end
    tsk(1,2).ScriptParams = sd;
    tsk(1,2).Static = true;
    tsk(1,1).Params = 1;
    tsk(1,2).Params = 2;

    res = PAsolve(tsk);

    val = PAwaitFor(res, timeout);
    disp('..........................1 ......OK');

    % Adding global selection script
    disp('...... Testing global selection script');

    oldsel = opt.CustomScript;
    oldstat = opt.CustomScriptStatic;
    oldparams = opt.CustomScriptParams;

    tsk(1,1).SelectionScript = [];
    tsk(1,1).Static = false;
    tsk(1,2).SelectionScript = [];


    PAoptions('CustomScript',fullfile(opt.MatSciDir, 'Tests', 'script','scriptwithparam.js'));
    PAoptions('CustomScriptStatic', false);
    xx = clock();
    sd='';
    for i=1:length(xx)
        sd=[sd num2str(xx(i)) '_'];
    end
    PAoptions('CustomScriptParams', sd);

    res = PAsolve(tsk);

    val = PAwaitFor(res, timeout);
    disp('..........................2 ......OK');

    PAoptions('CustomScript', oldsel);
    PAoptions('CustomScriptStatic',oldstat);
    PAoptions('CustomScriptParams', oldparams);

    ok = true;

    msg = [];
end