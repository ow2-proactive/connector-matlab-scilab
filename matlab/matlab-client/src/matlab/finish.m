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
opt = PAoptions();
try
    sched = PAScheduler;

    solver = sched.PAgetsolver();
    if strcmp(class(solver),'double')
        return;
    end

    if ischar(opt.SharedPushPublicUrl) & ~isempty(opt.SharedPushPublicUrl)
        % ok data is handled in a shared data space
    else
        if opt.EnableDisconnectedPopup
            trepository = org.ow2.proactive.scheduler.ext.matlab.client.embedded.MatlabTaskRepository.getInstance();
            notReceived = trepository.notYetReceived();
            if ~notReceived.isEmpty()
                  msg = ['The following jobs are not completed yet : '];
                  for j = 0:notReceived.size()-1
                      msg = [msg char(notReceived.get(j)) ' '];
                  end
                  msg = [msg 10];
                  msg = [msg 'The MiddleMan JVM can stay alive and handle data transfers while your computer is on' 10];
                  msg = [msg 'Do you want enable this mode ?'];
                  button = questdlg(msg,'Disconnect','Yes','No','Yes');
                  if strcmp(button, 'Yes')
                      return;
                  end
            end
        end
    end

    jvm = sched.PAgetJVMInterface();
    jvm.shutdown();

catch ME
    disp('There was a problem during the finish script. Displaying the error during 10 seconds...');
    if isa(ME,'MException')
        disp(getReport(ME));
    elseif isa(ME, 'java.lang.Throwable')
        ME.printStackTrace();
    end
    pause(10);
end




