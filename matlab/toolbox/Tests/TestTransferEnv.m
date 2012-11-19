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
function [ok, msg]=TestTransferEnv(nbiter,timeout)
if ~exist('timeout', 'var')
    if ispc()
        timeout = 200000;
    else
        timeout = 80000;
    end
end
if ~exist('nbiter', 'var')
    nbiter = 1;
end
opt=PAoptions();
old = opt.TransferEnv;
PAoptions('TransferEnv', true);
for kk=1:nbiter
    disp('-------------------------------------');
    disp(['------------------------Iteration '  num2str(kk)]);
    disp('...... Testing PAsolve with Transfer Env');
    disp('..........................1 Local');

        % declaring toto as a local variable that will be transfered
        toto = dummy('toto');
        resl = PAsolve(@transferenvfunc2,'ok');
        val=PAwaitFor(resl,timeout)
        if val
            disp('..........................1 ......OK');
            ok=true;
            msg = [];
        else
            disp('..........................1 ......KO');
            ok=false;
            msg = 'TestTransferEnv::wrong value for val, error occured remotely';
        end
    disp('..........................2 Local + Global');
    % declaring toto as a local variable that will be transfered
    toto = dummy('toto');
    % declaring totoGlobal in a subfunction as a global variable (and thus not
    % as a local variable)
    declareGlobal();
    resl = PAsolve(@transferenvfunc,'ok');
    val=PAwaitFor(resl,timeout)
    if val
        disp('..........................2 ......OK');
        ok=true;
        msg = [];
    else
        disp('..........................2 ......KO');
        ok=false;
        msg = 'TestTransferEnv::wrong value for val, error occured remotely';
    end

end
PAoptions('TransferEnv', old);
end

function out=declareGlobal()
global totoGlobal
totoGlobal = dummy('totoGlobal');
out='ok';
end