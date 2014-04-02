% PAoptions sets or returns the current options for the PAsolve execution
%
% Syntax
%
%       >> options = PAoptions();
%       >> PAoptions(param,value, ...);
%
% Inputs
%
%       param - a string containing the parameter name
%       value - the new value of this parameter
%
% Outputs
%
%       options - a structure with fields corresponding to parameters
%       names, containing all options values.
%
% Description
%       
%       PAoptions sets options used by the next PAsolve call. A structure
%       containing the current options can be retrieved by calling
%       PAoptions with no parameter.
%
% Example
%
%       >> PAoptions('Debug', true);
%       >> r = PAsolve(@factorial, 1, 2, 3, 4)  % Runs PAsolve in "Debug"
%       mode.
%
% Parameters
%
%   JobName
%               Name of the job that will be submitted to the Scheduler
%
%   JobDescription
%               Description of the job that will be submitted to the scheduler
%
%   Debug               true | false | 'on' | 'off'
%
%               Debug mode, default to 'off'
%
%
%   TransferEnv       true | false | 'on' | 'off'
%
%               Transfers the environment in which the PAsolve/PAeval function is called
%               to every remote tasks. Variables transferred this way need to be accessed inside the submitted function
%               via the evalin('caller', ...) syntax. Global variables are also transferred and can be accessed normally via the "global" keyword
%               default to 'off'
%   EnvExcludeList       
%               Comma separated list of variables which should be
%               excluded from the workspace when transferring the
%               environment (TransferEnv)
%   EnvExcludeTypeList
%               Comma separated list of object types which should be
%               excluded from the workspace when transferring the
%               environment (TransferEnv)
%
%   NbTaskExecution         integer >= 1
%               Defines how many times a task can be executed (in case of error),
%               it defaults to 2, to limit accidental crash of the remote engine due to memory limitations
%
%   Fork        true | false | 'on' | 'off'
%               Runs the tasks in a separate JVM process
%
%   RunAsMe     true | false | 'on' | 'off'
%               Runs the tasks under the account of the current user, default to 'off'
%
%   RemoveJobAfterRetrieve     true | false | 'on' | 'off'
%               Removes the job automatically after all results have been
%               retrieved. If the options is "off", the job is removed at the end of the
%               matlab session, or manually via PAjobRemove. default to
%               'on'
%
%   LicenseSaverURL           char
%               URL of the FlexNet LicenseSaver proxy. The LicenseSaver must be downloaded, installed and run separately
%               from the scheduler or from ProActive Matlab toolbox. Please send an email contact@activeeon.com .
%               The License Saver interacts with an existing FlexNet server to grab information about available Matlab and
%               Matlab toolboxes tokens. A Matlab task running inside ProActive Scheduler and connected to a LicenseSaver
%               will use tokens only when available, and will remain pending if no token is available.
%               If this configuration option is empty, no license check will be done.
%
%   CustomDataspaceURL        char
%               URL of the dataspace (both input and output) to expose, if
%               you don't want to rely on ProActive's automatic transfer
%               protocol. The dataspace server must of course be started
%               and configure manually in that case. e.g
%               ftp://myserver/rootpath
%
%   CustomDataspacePath       char
%               Path to the root of the Custom Dataspace provided by
%               CustomDataspaceURL. For example if ftp://myserver/rootpath
%               is the URL of the Dataspace and the rootpath directory
%               corresponds on the file system to /user/myserver/.../root
%               then this path must be specified in CustomDataspacePath.
%
%   SharedPushPublicUrl, SharedPullPublicUrl, SharedPushPrivateUrl, SharedPullPrivateUrl and SharedAutomaticTransfer    char & boolean
%               those url are those used by the Shared DataSpace on the scheduler (if it's activated on the scheduler).
%               The Push urls define the spaces where task input files are pushed to. The Pull urls are the spaces from
%               which task output files are pulled. The public urls are accessible from anywhere, the private urls are
%               accessible only by worker nodes. Contact the scheduler administrator to know these values. Both url (public and private)
%               can be equal, but if the infrastucture allows it, it is more efficient to use a file url as private,
%               the computing nodes will then access the space directly via a shared file system like NFS
%               The option SharedAutomaticTransfer is an internal option and should not be modified.
%
%   TransferMatFileOptions    char
%               If TranferEnv is set to on, tells which options are used to save the local environment
%               See the "save" command for more information. Default to
%               '-v7'
%
%   VersionPref       char
%               Determines the matlab version preferred to use by the worker, e.g. 7.5
%
%   VersionRej        char
%               A string containing a list of matlab versions that must not be used, delimiters can be spaces or commas : 7.5, 7.7
%
%   VersionMin        char
%               A minimum matlab version that can be used
%
%   VersionMax        char
%               A maximum matlab version that can be used
%
%   VersionArch        'any' | '32' | '64'
%               The matlab version architecture to be used. "Any" means any architecture can be used.
%
%   ForceMatlabSearch   boolean
%               Do we force the automated search of Matlab ?
%               In the default behavior, this is set to false. The selection script will try to search Matlab
%               only if a MatlabWorkerConfiguration.xml cannot be found on the host. If the script finds some Matlab instances
%               it will create a new MatlabWorkerConfiguration.xml to speed up latter executions.
%               If on the contrary ForceMatlabSearch is set to true, the selection script will always search the disk for
%               Matlab installations.
%
%   Priority          'Idle' | 'Lowest' | 'Low' | 'Normal' | 'High' | 'Highest'
%               Priority used by default for jobs submitted with PAsolve,
%               default to 'Normal'
%
%   UseJobClassPath
%               With this options set to true, the toolbox will use the jobClassPath feature of the scheduler, when submitting jobs
%               jar files necessary to the matlab workers will be copied at each task execution. It will not be necessary to put the
%               jars in the addons directory, but it will introduce an overhead
%
%   WindowsStartupOptions   char
%               Options given to matlab worker processes started on windows operating systems
%
%   LinuxStartupOptions     char
%               Options given to matlab worker processes started on linux operating systems
%
%   CustomScript
%               url or path of a user-defined selection script used in
%               addition to (before) FindMatlabScript and MatlabReservationScript
%
%   CustomScriptStatic
%               a boolean, true if the CustomScript is a static one
%               (executed only once on a given machine), otherwise the
%               CustomScript will be dynamic (default)
%
%   CustomScriptParams
%               a string containing the parameters of the custom script
%               delimited by spaces.
%
%   FindMatlabScript and FindMatSciScriptStatic
%               url or path of selection script used to find matlab
%               (internal)
%
%   MatlabReservationScript
%               url or path of selection script used to reserve matlab
%               tokens (internal)
%
%   ClientJarsDir, MiddlemanJarsDir and WorkerJarsDir
%               path to the jars used by the toolbox, middleman and worker (internal)
%               (internal)
%
%   ProActiveConfiguration
%               path to ProActive configuration file (internal)
%
%   Log4JConfiguration
%               path to log4j configuration file (internal)
%
%   SecurityFile
%               path to java security configuration file (internal)
%
%   RmiPort
%               default RMI port used when deploying the middleman JVM (internal)
%
%   JvmTimeout
%               default timeout used when deploying the middleman JVM (internal)
%
%   JvmArguments
%               Optional JVM arguments for the middleman JVM (internal)
%
%   UseMatlabControl
%               do we use the MatlabControl framework ? (internal)
%
%   EnableDisconnectedPopup
%               a popup will appear when the matlab session finishes and some jobs are uncomplete (internal)
%
%   WorkerTimeoutStart
%               Timeout used to start the matlab engine (*10ms) (internal)
%
%

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
function opts = PAoptions(varargin)

mlock
persistent pa_options

if  nargin == 0 && exist('pa_options','var') == 1 && ~isempty(pa_options)
    opts = pa_options;
    return;
elseif mod(nargin,2) ~= 0
    error(['Wrong number of arguments : ' num2str(nargin)]);
end

logcheck = @(x)(ischar(x) && ismember(x, {'on','off', 'true', 'false'})) || islogical(x) || (isnumeric(x) && ((x == 0)||(x == 1)));
versioncheck = @(x)((isnumeric(x)&&isempty(x)) || (ischar(x) && isempty(x)) || (ischar(x) && ~isempty(regexp(x, '^[1-9][\d]*\.[\d]+$'))));
versionlistcheck = @(x)((isnumeric(x)&&isempty(x)) || (ischar(x) && isempty(x)) || (ischar(x) &&  ~isempty(regexp(x, '^([1-9][\d]*\.[\d]+[ ;,]+)*[1-9][\d]*\.[\d]+$'))));

jarlistcheck = @(x)(ischar(x) &&  ~isempty(regexp(x, '^([\w\-\.]+\.jar[ ;,]+)*(([\w\-\.]+\.jar)|([*]))$')));
listcheck = @(x)(ischar(x) && (isempty(x) || ~isempty(regexp(x, '^([^ ;,]+[ ;,]+)*[^ ;,]+$'))));
listcheck2 = @(x)((isnumeric(x)&&isempty(x)) || (ischar(x) && (isempty(x) || ~isempty(regexp(x, '^([^ ]+[ ]+)*[^ ]+$')))));
listtrans = @listtocell;
listtrans2 = @listtocell2;

urlcheck=@(x)((isnumeric(x)&&isempty(x)) || ischar(x));

charornull = @(x)((isnumeric(x)&&isempty(x)) || ischar(x));

charornum = @(x)(isnumeric(x) || ischar(x));


v = version;
[maj,v] = strtok(v,'.');
[min,v] = strtok(v,'.');



[pathstr, name, ext] = fileparts(mfilename('fullpath'));
javafile = java.io.File(pathstr);
matsci_dir = char(javafile.getCanonicalPath());
tmp_dir = char(java.lang.System.getProperty('java.io.tmpdir'));
home_dir = char(java.lang.System.getProperty('user.home'));
logtrans = @(x)((islogical(x) && x) || (ischar(x) && (strcmp(x,'on') || strcmp(x,'true'))) || (isnumeric(x)&&(x==1)));
conftrans = @(x)(strrep(variabletrans(x),'/',filesep));

ischarornull = @(x)(ischar(x) || isnumeric(x)&&isempty(x));
ss = @(x)str2num(x);
isstrictpositiveint = @(x)(isnumeric(ss(x)) && isscalar(ss(x)) && ss(x) > 0 && floor(ss(x)) == ss(x));

id = @(x)x;


j=1;
inputs(j).name = 'JobName';
inputs(j).default = 'Matlab';
inputs(j).check = @ischar;
inputs(j).trans = id;
j=j+1;
inputs(j).name = 'JobDescription';
inputs(j).default = 'Set of parallel Matlab tasks';
inputs(j).check = @ischar;
inputs(j).trans = id;
j=j+1;
inputs(j).name = 'Debug';
inputs(j).default = false;
inputs(j).check = logcheck;
inputs(j).trans = logtrans;
j=j+1;
inputs(j).name = 'TimeStamp';
inputs(j).default = false;
inputs(j).check = logcheck;
inputs(j).trans = logtrans;
j=j+1;
inputs(j).name = 'LicenseSaverURL';
inputs(j).default = [];
inputs(j).check = urlcheck;
inputs(j).trans = id;
j=j+1;
inputs(j).name = 'Fork';
inputs(j).default = false;
inputs(j).check = logcheck;
inputs(j).trans = logtrans;
j=j+1;
inputs(j).name = 'RunAsMe';
inputs(j).default = false;
inputs(j).check = logcheck;
inputs(j).trans = logtrans;
j=j+1;
inputs(j).name = 'SharedPushPublicUrl';
inputs(j).default = [];
inputs(j).check = urlcheck;
inputs(j).trans = id;
j=j+1;
inputs(j).name = 'SharedPullPublicUrl';
inputs(j).default = [];
inputs(j).check = urlcheck;
inputs(j).trans = id;
j=j+1;
inputs(j).name = 'SharedPushPrivateUrl';
inputs(j).default = [];
inputs(j).check = urlcheck;
inputs(j).trans = id;
j=j+1;
inputs(j).name = 'SharedPullPrivateUrl';
inputs(j).default = [];
inputs(j).check = urlcheck;
inputs(j).trans = id;
j=j+1;
inputs(j).name = 'SharedAutomaticTransfer';
inputs(j).default = true;
inputs(j).check = logcheck;
inputs(j).trans = logtrans;
j=j+1;
inputs(j).name = 'RemoveJobAfterRetrieve';
inputs(j).default = false;
inputs(j).check = logcheck;
inputs(j).trans = logtrans;
j=j+1;
inputs(j).name = 'NbTaskExecution';
inputs(j).default = 2;
inputs(j).check = isstrictpositiveint;
inputs(j).trans = ss;
j=j+1;
inputs(j).name = 'TransferEnv';
inputs(j).default = false;
inputs(j).check = logcheck;
inputs(j).trans = logtrans;
j=j+1;
inputs(j).name = 'EnvExcludeList';
inputs(j).default = '';
inputs(j).check = listcheck;
inputs(j).trans = listtrans;
j=j+1;
inputs(j).name = 'EnvExcludeTypeList';
inputs(j).default = '';
inputs(j).check = listcheck;
inputs(j).trans = listtrans;
j=j+1;
inputs(j).name = 'CustomDataspaceURL';
inputs(j).default = [];
inputs(j).check = urlcheck;
inputs(j).trans = id;
j=j+1;
inputs(j).name = 'CustomDataspacePath';
inputs(j).default = [];
inputs(j).check = charornull;
inputs(j).trans = id;
j=j+1;
inputs(j).name = 'TransferMatFileOptions';
inputs(j).default = '-v7';
inputs(j).check = charornull;
inputs(j).trans = id;
j=j+1;
inputs(j).name = 'VersionPref';
inputs(j).default = [maj '.' min];
inputs(j).check = versioncheck;
inputs(j).trans = @versiontrans;
j=j+1;
inputs(j).name = 'VersionRej';
inputs(j).default = [];
inputs(j).check = versionlistcheck;
inputs(j).trans = id;
j=j+1;
inputs(j).name = 'VersionMin';
inputs(j).default = [];
inputs(j).check = versioncheck;
inputs(j).trans = @versiontrans;
j=j+1;
inputs(j).name = 'VersionMax';
inputs(j).default = [];
inputs(j).check = versioncheck;
inputs(j).trans = @versiontrans;
j=j+1;
inputs(j).name = 'VersionArch';
inputs(j).default = 'any';
inputs(j).check = @(x)(ischar(x) && ismember(x, {'any', '32', '64', }));
inputs(j).trans = id;
j=j+1;
inputs(j).name = 'ForceMatlabSearch';
inputs(j).default = false;
inputs(j).check = logcheck;
inputs(j).trans = logtrans;
j=j+1;
inputs(j).name = 'MatlabReservationScript';
inputs(j).default = ['$MATSCI$' filesep 'script' filesep 'reserve_matlab.rb' ];
inputs(j).check = @ischar;
inputs(j).trans = @scripttrans;
j=j+1;
inputs(j).name = 'FindMatlabScript';
inputs(j).default = ['$MATSCI$' filesep 'script' filesep 'file_matlab_finder.rb' ];
inputs(j).check = @ischar;
inputs(j).trans = @scripttrans;
j=j+1;
inputs(j).name = 'CustomScript';
inputs(j).default = [];
inputs(j).check = ischarornull;
inputs(j).trans = @scripttrans;
j=j+1;
inputs(j).name = 'CustomScriptStatic';
inputs(j).default = false;
inputs(j).check = logcheck;
inputs(j).trans = logtrans;
j=j+1;
inputs(j).name = 'CustomScriptParams';
inputs(j).default = [];
inputs(j).check = ischarornull;
inputs(j).trans = id;
j=j+1;
inputs(j).name = 'FindMatSciScriptStatic';
inputs(j).default = true;
inputs(j).check = logcheck;
inputs(j).trans = logtrans;
j=j+1;
inputs(j).name = 'Priority';
inputs(j).default = 'Normal';
inputs(j).check = @(x)(ischar(x) && ismember(x, {'Idle', 'Lowest', 'Low', 'Normal', 'High', 'Highest'}));
inputs(j).trans = id;
j=j+1;
inputs(j).name = 'UseJobClassPath';
inputs(j).default = true;
inputs(j).check = logcheck;
inputs(j).trans = logtrans;
j=j+1;
inputs(j).name = 'WindowsStartupOptions';
inputs(j).default = '-automation -nodesktop -nosplash -nodisplay';
inputs(j).check = @ischar;
inputs(j).trans = id;
j=j+1;
inputs(j).name = 'LinuxStartupOptions';
inputs(j).default = '-nodesktop -nosplash -nodisplay';
inputs(j).check = @ischar;
inputs(j).trans = id;
j=j+1;
inputs(j).name = 'ClientJarsDir';
inputs(j).default = ['$MATSCI$'  filesep  'lib' filesep 'client'];
inputs(j).check = @ischar;
inputs(j).trans = conftrans;
j=j+1;
inputs(j).name = 'MiddlemanJarsDir';
inputs(j).default = ['$MATSCI$'  filesep  'lib' filesep 'middleman'];
inputs(j).check = @ischar;
inputs(j).trans = conftrans;
j=j+1;
inputs(j).name = 'WorkerJarsDir';
inputs(j).default = ['$MATSCI$'  filesep  'lib' filesep 'worker'];
inputs(j).check = @ischar;
inputs(j).trans = conftrans;
j=j+1;
inputs(j).name = 'ProActiveConfiguration';
inputs(j).default = ['$MATSCI$' filesep 'config' filesep 'MatlabProActiveConfiguration.xml'];
inputs(j).check = @ischar;
inputs(j).trans = conftrans;
j=j+1;
inputs(j).name = 'Log4JConfiguration';
inputs(j).default = ['$MATSCI$' filesep 'config' filesep 'log4j-client'];
inputs(j).check = @ischar;
inputs(j).trans = conftrans;
j=j+1;
inputs(j).name = 'SecurityFile';
inputs(j).default = ['$MATSCI$' filesep 'config' filesep 'security.java.policy-client'];
inputs(j).check = @ischar;
inputs(j).trans = conftrans;
j=j+1;
inputs(j).name = 'UseMatlabControl';
inputs(j).default = false;
inputs(j).check = logcheck;
inputs(j).trans = logtrans;
j=j+1;
inputs(j).name = 'EnableDisconnectedPopup';
inputs(j).default = true;
inputs(j).check = logcheck;
inputs(j).trans = logtrans;
j=j+1;
inputs(j).name = 'MatSciDir';
inputs(j).default = matsci_dir;
inputs(j).check = @ischar;
inputs(j).trans = id;
j=j+1;
inputs(j).name = 'RmiPort';
inputs(j).default = 1111;
inputs(j).check = charornum;
inputs(j).trans = @charornumtrans;
j=j+1;
inputs(j).name = 'JvmArguments';
inputs(j).default = [];
inputs(j).check = listcheck2;
inputs(j).trans = listtrans2;
j=j+1;
inputs(j).name = 'JvmTimeout';
inputs(j).default = 1200;
inputs(j).check = charornum;
inputs(j).trans = @charornumtrans;
j=j+1;
inputs(j).name = 'WorkerTimeoutStart';
inputs(j).default = 6000;
inputs(j).check = charornum;
inputs(j).trans = @charornumtrans;

function s = variabletrans(x)
if ~ischar(x)
    s = x;
else
s  = strrep(strrep(strrep(x, '$MATSCI$', matsci_dir),'$TMP$',tmp_dir), '$HOME$', home_dir);
end
end

function s = scripttrans(x)
if ~ischar(x)
    s = x;
else
    s = ['file:' strrep(variabletrans(x), '\', '/')];
end
end

function num = charornumtrans(x)
if ischar(x)
    num = str2num(x);
else
    num = x;
end
end

function v = versiontrans(x)
if ischar(x) && isempty(x)
    v = [];
else
    v = x;
end
end

function cl=listtocell(x)
cl={};
i=1;
remain=x;
if iscell(x)
    cl = x;
    return;
end
goon=true;
while goon
    [str, remain] = strtok(remain, ',; ');
    if isempty(str) || length(str) == 0
        goon=false;
    else
        cl{i}=str;
        i=i+1;
    end
end
end

function cl=listtocell2(x)
cl={};
i=1;
remain=x;
if iscell(x)
    cl = x;
    return;
end
goon=true;
while goon
    [str, remain] = strtok(remain, ' ');
    if isempty(str) || length(str) == 0
        goon=false;
    else
        cl{i}=str;
        i=i+1;
    end
end
end




% Parsing option file
if ~exist('pa_options','var') == 1 || ~isstruct(pa_options)
    userdir = char(java.lang.System.getProperty('user.home'));
    optionpath = [userdir filesep '.matlab' filesep 'PAoptions.ini'];
    if exist(optionpath, 'file');
        fid = fopen(optionpath, 'r');
    else
        optionpath = [matsci_dir filesep 'config', filesep, 'PAoptions.ini'];
        if ~exist(optionpath, 'file');
            error(['cannot find option file ' optionpath]);
        end
        fid = fopen(optionpath, 'r');
    end
    try
        C = textscan(fid, '%s = %[^\n]', 'CommentStyle', '%%');
        for i=1:length(C{1})
            for j=1:length(inputs)
                if strcmp(C{1}{i},inputs(j).name)
                    chk = inputs(j).check;
                    tf = chk(C{2}{i});
                    if ~tf
                        error(['Parse error when loading option file ' optionpath ', option ' C{1}{i} ' doesn''t satisfy check ' func2str(chk) ]);
                    end
                    trans = inputs(j).trans;
                    pa_options = setfield(pa_options, inputs(j).name, trans(C{2}{i}));                    
                end
            end
        end
    catch ME
        fclose(fid);
        throw(ME);
    end
    fclose(fid);
end


for i = 1:length(inputs)
    default = true;
    trans = inputs(i).trans;
    Parameter = inputs(i).default;
    for j= 1:nargin/2
        optionName = varargin{2*(j-1)+1};
        value = varargin{2*j};

        if strcmp(inputs(i).name, optionName)
            chk = inputs(i).check;
            tf = chk(value);
            if ~tf
                error(['Argument ' optionName ' doesn''t satisfy check ' func2str(chk) ]);
            end
            default = false;
            Parameter = trans(value);
        end
    end
    if ~default || ~(isstruct(pa_options) && isfield(pa_options,inputs(i).name))
        pa_options = setfield(pa_options, inputs(i).name, Parameter);
    end
end

opts = pa_options;

end



