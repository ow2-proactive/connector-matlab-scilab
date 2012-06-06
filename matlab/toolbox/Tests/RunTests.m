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
function [ok, msg]=RunTests(nbiter,timeout)

runimage = false;
runsignal = false;
runbigarray = false;



if exist('timeout', 'var')
    [ok,msg] = TestBasic(nbiter,timeout);
else
    [ok,msg] = TestBasic(nbiter);
end
if ~ok disp(msg),return; end
if exist('timeout', 'var')
    [ok,msg] = TestCompose(nbiter,timeout);
else
    [ok,msg] = TestCompose(nbiter);
end
if ~ok disp(msg),return; end
if exist('timeout', 'var')
    [ok,msg] = TestObjectArguments(nbiter,timeout);
else
    [ok,msg] = TestObjectArguments(nbiter);
end
if ~ok disp(msg),return; end
if exist('timeout', 'var')
    [ok,msg] = TestTransferEnv(nbiter,timeout);
else
    [ok,msg] = TestTransferEnv(nbiter);
end
if ~ok disp(msg),return; end
if runbigarray
    if exist('timeout', 'var')
        [ok,msg] = TestBigArrayAndKeepEngine(nbiter,timeout);
    else
        [ok,msg] = TestBigArrayAndKeepEngine(nbiter);
    end
    if ~ok disp(msg),return; end
end
if runimage
    if exist('timeout', 'var')
        [ok,msg] = TestPATask(nbiter,timeout);
    else
        [ok,msg] = TestPATask(nbiter);
    end
end
if ~ok disp(msg),return; end
if runsignal
    if exist('timeout', 'var')
        [ok,msg] = TestSignal(nbiter,timeout);
    else
        [ok,msg] = TestSignal(nbiter);
    end
end
if ~ok disp(msg),return; end
if exist('timeout', 'var')
    [ok,msg] = TestMultipleSubmit(nbiter,timeout);
else
    [ok,msg] = TestMultipleSubmit(nbiter);
end
if ~ok disp(msg),return; end
if exist('timeout', 'var')
    [ok,msg] = TestDummyDisconnected(nbiter,timeout);
else
    [ok,msg] = TestDummyDisconnected(nbiter);
end
if ~ok disp(msg),return; end
if exist('timeout', 'var')
    [ok,msg] = TestTopology(nbiter,timeout);
else
    [ok,msg] = TestTopology(nbiter);
end
if ~ok disp(msg),return; end
if exist('timeout', 'var')
    [ok,msg] = TestSelectionScripts(nbiter,timeout);
else
    [ok,msg] = TestSelectionScripts(nbiter);
end
if ~ok disp(msg),return; end

