% PAFile constructor of PAFile objects
%
% Syntax
%       t = PAFile(nbitems);
%
% Inputs
%       
%       nbitems - the number of items in this PAFile list
%
% Outputs
%       
%       t - a PAFile list
%
% Properties 
%
%       Path - a matlab function handle (the function to execute remotely)
%       
%       Space - the name of the space used for this file, it can be 'automatic', 'input', 'output', or 'global'
%
% Examples
%
%       >> f = PAFiles(2);
%       >> f.Space = 'automatic'
%       >> f(1).Path = 'intputfile1.mat';
%       >> f(2).Path = 'intputfile2.mat';
%
%
% See also
%       PATask


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
function varargout = PAFile(nbitems)
if exist('nbitems','var') == 1
    for i=1:nbitems
            this(i) = PAFile();
    end  
else
    this.Path = [];
    this.Space = 'automatic';
end
for i=1:nargout
    varargout{i}=[];
    if exist('nbitems','var') == 1
        varargout{i} = this;
    else
        varargout{i} = class(this,'PAFile');
    end
end

