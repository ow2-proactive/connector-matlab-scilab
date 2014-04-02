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
function varargout = PAprepare()

mlock
persistent PA_Prepared
if exist('PA_Prepared','var') == 1 && ~isempty(PA_Prepared) && PA_Prepared == 1
else
    % Setting the English locale, mandatory for ptolemy
    java.util.Locale.setDefault(java.util.Locale.ENGLISH);
    
    [pathstr, name, ext] = fileparts(mfilename('fullpath'));
    
    % Scheduler root
    opt=PAoptions;
    matsci_dir = opt.MatSciDir;
    
    
    
    % Dist libs
    fs=filesep();
    client_lib_dir = opt.ClientJarsDir;
    if ~exist(client_lib_dir,'dir')
        error(['PAprepare::cannot find directory defined in option ClientJarsDir : ' client_lib_dir]);
    end
    
    
    
    jcp = javaclasspath();
                
    
    % script engines must be at the beginning to avoid jar index problems
    dir_content = dir([client_lib_dir filesep '*.jar']);
    
    warning('off')
    
    for i=1:length(dir_content)
        javaaddpath([client_lib_dir filesep dir_content(i).name],'-END');
    end
    
    java.lang.System.setProperty('java.rmi.server.RMIClassLoaderSpi','default');
    
    warning('on')
    
    
    
end

    function out = shortname(in, match)
        while true
            [str, in] = strtok(in);
            if isempty(str),  break;  end
            if strcmp(str, match)
                out = old_str;
            end
            old_str = str;
        end
    end

PA_Prepared = 1;

end
