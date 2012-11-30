function jobs = PAconnect(url, credpath)
% PAconnect connects to the ProActive scheduler
%
% Syntax
%
%       PAconnect();
%       PAconnect(url [,credpath]);
%       jobs = PAconnect(url [,credpath]);
%
% Inputs
%
%       url - url of the scheduler
%       credpath - path to the login credential file
%
% Ouputs
%
%       jobs - id of jobs that were not terminated at matlab's previous
%       shutdown
%
% Description
%
%       PAconnect connects to a running ProActive Scheduler by specifying its url. If the scheduler could be reached a popup
%       window will appear, asking for login and password. An additional SSH key can also be provided when the user needs
%       to execute remote task under one's identity (RunAsMe option). ProActive Scheduler features a full account management
%       facility along with the possibility to synchronize to existing Windows or Linux accounts via LDAP. More information can be found inside
%       Scheduler's manual chapter "Configure users authentication". If you haven't configured any account in the scheduler
%       use, the default account login "demo", password "demo".
%       You can as well encrypt credentials using the command line tool "create-cred" and provide the path to the credentials
%       file with the parameter credpath. Or you can simply reuse automatically the same credentials and url that you used at
%       last scheduler connection by using PAconnect() without parameter.
%       PAconnect() without parameter can also be used to connect to a local ProActive scheduler deployed with the standard
%       RMI protocol
%
%
%       In case jobs from the last Matlab session were not complete before Matlab exited. It is possible to get their result
%       using PAgetResults
%
% Example
%
%   PAconnect('rmi://scheduler:1099')
%
% See also
%   PAsolve, PAgetResults
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

mlock
persistent PA_scheduler_URI

sched = PAScheduler;
opt = PAoptions();
jobs = [];

% Verify that proactive is already on the path or not
p = javaclasspath('-all');
cptoadd = 1;
for i = 1:length(p)
    if (strfind(p{i}, 'ProActive_Scheduler-matlabemb.jar'))
        cptoadd = 0;
    end
end
if cptoadd == 1
    sched.PAprepare();
end
reconnected = false;

% Test that the session is not already connected to a Scheduler, or that
% the connection to it is failing
tmpsolver = sched.PAgetsolver();
if ~strcmp(class(tmpsolver), 'double')
    isJVMdeployed = 1;
    isConnected = 0;
    try
        isConnected = tmpsolver.isConnected();
    catch
        isJVMdeployed = 0;
    end
else
    isJVMdeployed = 0;
    isConnected = 0;
end

if ~exist('url', 'var') == 1
    url = [];
end
if exist('PA_scheduler_URI','var') == 1 && ~isempty(PA_scheduler_URI) && ~isempty(url) && ~strcmp(PA_scheduler_URI,url)
    % particular case when the scheduler uri changes, we redeploy everything
    isJVMdeployed = 0;
    isConnected = 0;
else
    PA_scheduler_URI = url;
end


if ~isJVMdeployed
    % Creating a new connection
    deployJVM(sched,opt,url);
end
if ~isConnected
    % joining the scheduler
    solver = sched.PAgetsolver();
    ok = solver.join(url);
    if ~ok
        logPath = solver.getLogFilePath();
        error(['PAconnect::Error ProActive Scheduler cannot be contacted at URL ' url ', detailed error message has been logged to ' char(logPath)]);
    end
    jobs = dataspaces(sched, opt);
else
    solver = tmpsolver;
end

if solver.isLoggedIn()
    error('This session is already connected to a scheduler.');
end

if exist('credpath', 'var')
    login(solver, sched, url, credpath);
else
    login(solver, sched, url);
end



end

function deployJVM(sched,opt,url)
deployer = org.ow2.proactive.scheduler.ext.matlab.client.embedded.util.MatlabJVMSpawnHelper.getInstance();
home = getenv('JAVA_HOME');
fs=filesep();
if length(home) > 0
    if ispc()
        deployer.setJavaPath([home fs 'bin' fs 'java.exe']);
    else
        deployer.setJavaPath([home fs 'bin' fs 'java']);
    end
    
end
[pathstr, name, ext] = fileparts(mfilename('fullpath'));
javafile = java.io.File(pathstr);
matsci_dir = opt.MatSciDir;

dist_lib_dir = [matsci_dir fs 'lib'];
if ~exist(dist_lib_dir,'dir')
    error(['PAconnect::cannot find directory ' dist_lib_dir]);
end
jars = opt.ProActiveJars;
jarsjava = javaArray('java.lang.String', length(jars));
for i=1:length(jars)
    jarsjava(i) = java.lang.String([dist_lib_dir filesep jars{i}]);
end
options = opt.JvmArguments;
for i=1:length(options)
    deployer.addJvmOption(options{i});
end
deployer.setMatSciDir(matsci_dir);
deployer.setSchedulerURI(url);
deployer.setDebug(opt.Debug);
deployer.setClasspathEntries(jarsjava);
deployer.setProActiveConfiguration(opt.ProActiveConfiguration);
deployer.setLog4JFile(opt.Log4JConfiguration);
deployer.setPolicyFile(opt.SecurityFile);
deployer.setClassName('org.ow2.proactive.scheduler.ext.matlab.middleman.MatlabMiddlemanDeployer');


rmiport = opt.RmiPort;

deployer.setRmiPort(rmiport);

pair = deployer.deployOrLookup();
itfs = pair.getX();
PAoptions('RmiPort',pair.getY());
solver = deployer.getMatlabEnvironment();
sched.PAgetsolver(solver);
registry = deployer.getDSRegistry();
sched.PAgetDataspaceRegistry(registry);
jvmint = deployer.getJvmInterface();
sched.PAgetJVMInterface(jvmint);

end

function login(solver, sched, url, credpath)
opt = PAoptions();
% Logging in
if exist('credpath', 'var')
    try
        solver.login(credpath);
    catch ME
        disp(getReport(ME));
        error('PAconnect::Authentication error');
    end
elseif isempty(url) && solver.hasCredentialsStored()
    try
        solver.login([], [], []);
    catch ME
        disp(getReport(ME));
        error('PAconnect::Authentication error');
    end
else
    disp('Connection successful, please enter login/password');
    loggedin = false;
    msg = 'Connect to the Scheduler';
    attempts = 1;
    while ~loggedin && attempts <= 3
        [login,pwd,keyfile]=sched.logindlg('Title',msg);
        try
            solver.login(login,pwd,keyfile);
            loggedin = true;
        catch ME
            if opt.Debug
                disp(getReport(ME));
            end
            attempts = attempts+1;
            msg = ['Incorrect Login/Password, try ' num2str(attempts)];
        end
    end
    if attempts > 3
        error('PAconnect::Authentication error');
    end
    sched.PAgetlogin(login);
    
end
disp('Login successful');

end

function jobs = dataspaces(sched,opt)
% Dataspace Handler
jobs = [];
registry = sched.PAgetDataspaceRegistry();
registry.init('MatlabInputSpace', 'MatlabOutputSpace', opt.Debug);
trepository = org.ow2.proactive.scheduler.ext.matlab.client.embedded.MatlabTaskRepository.getInstance();
notReceived = trepository.notYetReceived();
if ~notReceived.isEmpty()
    msg = ['The following jobs were uncomplete before last matlab shutdown : '];
    for j = 0:notReceived.size()-1
        jid = char(notReceived.get(j));
        msg = [msg ' ' jid];
        jobs{j+1} = jid;
    end
    disp(msg);
end
end




