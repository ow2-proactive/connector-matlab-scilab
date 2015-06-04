% PAsolve run matlab functions remotely
%
% Syntax
%
%       Basic:
%       >> results = PAsolve(func, arg_1, arg_2, ..., arg_n);
%
%       Advanced:
%       >> results = PAsolve(patask_1(1..k), patask_2(1..k), ... ,
%       PATask_n(1..k));
%       >> results = PAsolve(patask(1..n,1..k));
%
% Inputs
%
%       func - a matlab function handle
%
%       arg_k - a parameter to the "func" function if func takes only one
%       parameter as input OR a cell-array containing the list of
%       parameters to func.
%
%       patask_k - a vector of PATask objects
%
%       patask - a matrix of PATask objects
%
%
% Description
%
%       The call to PAsolve is synchronous until the scheduler has received the
%       information necessary to run the tasks. PAsolve returns right
%       afterwards and doesn't block matlab until the tasks have been scheduled
%       and completed.
%
%       PAsolve returns an array of objects of type PAResult. Its size matches
%       the number of argk or pataskk given or the number of columns in the
%       patask matrix.
%
%       Blocking wait functions can be called on this PAResult array or on
%       a portion of this array (see PAwaitFor, PAwaitAny). Non-blocking
%       functions can also be called to know if a result is available
%       (PAisAwaited)
%
%       PAsolve is based on the principle of parametric sweep, i.e. one
%       task/many parameters (see Basic syntax).
%
%       PAsolve can either be called by giving a function handle and a list
%       of parameters (Basic Syntax), or by providing arrays of PATask objects which
%       allows more advanced parametrization of the execution (see PATask).
%
%       The semantic of execution for PATask matrices is that each column
%       will be executed separately, and within each column each line will
%       be execute sequentially and thus will depend on the execution of
%       the previous line.
%
%       PAsolve behaviour can be configured using the PAoptions function.
%
%
% See also
%       PAconnect, PAoptions, PAgetResults, PATask, PAResult, PAResult/PAwaitFor,
%       PAResult/PAwaitAny, PAResult/PAisAwaited
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
function results = PAsolve(varargin)

    function [names,types] = filternames(names, types)
        names_ex = opt.EnvExcludeList;
        types_ex = opt.EnvExcludeTypeList;
        j=1;
        for i=1:length(names)
            tf1 = ismember(names{j}, names_ex);
            tf2 = ismember(types{j}, types_ex);
            if tf1
                names(j)=[];
                types(j)=[];
            elseif tf2
                names(j)=[];
                types(j)=[];
            else
                j=j+1;
            end
        end
    end


% Checking the parameters
[Tasks, NN, MM]=parseParams(varargin{:});

sched = PAScheduler;
% Get the solver from memory
solver = sched.PAgetsolver();
if strcmp(class(solver),'double')
    error('A connection to the ProActive scheduler must be established in order to use PAsolve, see PAconnect.');
end

PAensureConnected();


opt = PAoptions;



trepository = org.ow2.proactive.scheduler.ext.matlab.client.embedded.MatlabTaskRepository.getInstance();


% we check if there was a previously recorded job
recordedJobInfo = trepository.getNextJob();
if ~isjava(recordedJobInfo)

    solve_config = org.ow2.proactive.scheduler.ext.matlab.common.data.PASolveMatlabGlobalConfig();
    task_config = javaArray('org.ow2.proactive.scheduler.ext.matlab.common.data.PASolveMatlabTaskConfig', NN,MM);
    for i=1:NN
        for j=1:MM
            task_config(i,j)= org.ow2.proactive.scheduler.ext.matlab.common.data.PASolveMatlabTaskConfig();
        end
    end

    % Checking all functions
    [funcDatabase,allfuncs] = initFunctions(Tasks,task_config, NN, MM,sched);


    % Temp directories
    [globalFilesToClean,taskFilesToClean,pa_dir,curr_dir,fs,subdir, solveid] = initDirectories(opt,solve_config,NN);

    % Initializing data spaces
    initDS(opt,sched,solve_config,curr_dir);

    % Initialize Remote functions
    [keepaliveFunctionName,checktoolboxesFunctionName]= initRemoteFunctions(solve_config);

    % Transfering the environment
    if opt.TransferEnv
        envMatName = ['MatlabPASolveEnv_' num2str(solveid) '.mat'];
        envFilePath = [pa_dir fs envMatName];

        % saving caller workspace and global variables
        local_varlist = evalin('caller', 'whos();');

        local_names = {local_varlist.name};
        local_types = {local_varlist.class};

        [local_names,local_types] = filternames(local_names, local_types);

        % additional processing for specific types
        for i=1:length(local_names)
            if evalin('caller', ['iscom(' local_names{i} ');'])
                local_types{i} = 'com';
            elseif evalin('caller', ['isjava(' local_names{i} ');'])
                local_types{i} = 'java';
            elseif evalin('caller', ['isinterface(' local_names{i} ');'])
                local_types{i} = 'interface';
            end
        end

        global_varlist = whos('global');
        global_names = {global_varlist.name};
        global_types = {global_varlist.class};

        [global_names,global_types] = filternames(global_names, global_types);

        local_names_mod = cellfun(@(x) ['''' x ''','] ,local_names,'UniformOutput',false);
        local_names_str = cell2mat(local_names_mod);
        if opt.Debug
            disp(['Saving caller vars :' local_names_str ' in ' envFilePath]);
        end
        evalin('caller', ['save(''' envFilePath  ''',' local_names_str '''' opt.TransferMatFileOptions ''')']);

        global_names_mod = cellfun(@(x) [' ' x] ,global_names,'UniformOutput',false);
        global_names_str = cell2mat(global_names_mod);

        global_names_mod2 = cellfun(@(x) ['''' x ''','] ,global_names,'UniformOutput',false);
        global_names_str2 = cell2mat(global_names_mod2);

        if length(global_names_str) > 0
             eval(['global' global_names_str]);
        end

        for i=1:length(global_names)
            if eval(['iscom(' global_names{i} ');'])
                global_types{i} = 'com';
            elseif eval(['isjava(' global_names{i} ');'])
                global_types{i} = 'java';
            elseif eval(['isinterface(' global_names{i} ');'])
                global_types{i} = 'interface';
            end
        end

        if opt.Debug && length(global_names_str) > 0
            disp(['Saving global vars :' global_names_str ' in ' envFilePath]);
        end
        if length(global_names_str2) > 0
            eval(['save(''' envFilePath  ''',' global_names_str2 ' ''-append'',''' opt.TransferMatFileOptions ''')']);
        end

        globalNames = [];

        if length(global_names) > 0
            globalNames = javaArray('java.lang.String', length(global_names));
            for i=1:length(global_names)
                globalNames(i) = java.lang.String(global_names{i});
            end
        end


        solve_config.setEnvMatFile(subdir, envMatName, globalNames)

        % globalFilesToClean=[globalFilesToClean {envFilePath}];
    else
        local_names = {};
        local_types = {};
        global_names = {};
        global_types = {};
    end

    % Transfering source files
    [funcDatabase,taskFilesToClean] = initTransferSource(opt,fs,solveid,funcDatabase,sched,allfuncs,...
        NN,MM,Tasks,keepaliveFunctionName,checktoolboxesFunctionName,taskFilesToClean,task_config,pa_dir,subdir, local_names,local_types, global_names, global_types);

    % Init Input Files
    [taskFilesToClean] = initInputFiles(NN,MM,Tasks,opt,fs,taskFilesToClean,task_config);

    % Init Output Files
    [taskFilesToClean] = initOutputFiles(NN,MM,Tasks,opt,subdir,pa_dir,taskFilesToClean,task_config);

    % Init Other attributes
    initOtherTCAttributes(NN,MM, task_config, Tasks);

    % Init Parameters
    [input,main,taskFilesToClean,outVarFiles]=initParameters(solveid,NN,MM,Tasks,opt,taskFilesToClean,task_config,allfuncs,pa_dir,subdir,fs);

    % Init Solve Config
    initSolveConfig(solve_config,opt,sched);

    % Send the task list to the scheduler

    jobinfo = solver.solve(solve_config, task_config);

    % if null is returned there was a connection problem, we reconnect and retry
    if ~isjava(jobinfo)
        PAensureConnected();
        jobinfo = solver.solve(solve_config, task_config);
        % outputs = PAsolve(varargin);
    end

    jid = char(jobinfo.getJobId());
    disp(['Job submitted : ' jid]);
    trepository.addJob(jobinfo);

else
    solve_config = recordedJobInfo.getGlobalConfig();
    task_configs = recordedJobInfo.getTaskConfigs();


    jobinfo = solver.solve(solve_config, task_configs);
     % if null is returned there was a connection problem, we reconnect and retry
     if ~isjava(jobinfo)
         PAensureConnected();
         jobinfo = solver.solve(solve_config, task_configs);
         % outputs = PAsolve(varargin);
     end

     jid = char(jobinfo.getJobId());
     disp(['Job recalled : ' jid]);
end

dir_to_clean = char(jobinfo.getDirToClean());

ftn = jobinfo.getFinalTaskNames();
tnit = ftn.iterator();
for i=1:NN
    taskinfo.cleanDir = dir_to_clean;
    out_path = jobinfo.getOutputVariablePathWithIndex(i-1);
    taskinfo.outFile = char(out_path);
    taskinfo.jobid = jid;
    taskinfo.taskid = char(tnit.next());
    results(i)=PAResult(taskinfo);
end

end

% Parse command line parameters
function [Tasks, NN, MM]=parseParams(varargin)

if length(varargin) == 0
   error('Parameter list cannot be empty');
end

if isa(varargin{1}, 'function_handle')
    Func = varargin{1};
    NN=length(varargin)-1;
    if NN == 0
        error('Parameter list cannot be empty');
    end
    Tasks(1:NN) = PATask;
    Tasks(1:NN).Func = Func;
    for i=1:NN
        if isa(varargin{i+1}, 'PATask')
            error('PATask parameters not supported with a function_handle as first argument.');
        end
        Tasks(i).Params = varargin{i+1};
    end
    MM = 1;
elseif isa(varargin{1}, 'PATask')
    if length(varargin) == 1
        Tasks = varargin{1};
        NN = size(Tasks,2);
        if NN == 0
            error('PATask array cannot be empty');
        end
        MM = size(Tasks,1);
    else
        NN=length(varargin);
        MM = -1;
        for i=1:NN
            if isa(varargin{i}, 'PATask')
                if (size(varargin{i},2) ~= 1)
                    error(['parameter ' num2str(i) ' should be a column vector.']);
                end
                sz = size(varargin{i},1);
                if MM == -1
                    MM = sz;
                elseif MM ~= sz
                    error(['parameter ' num2str(i) ' should be a column vector of the same length than other parameters.']);
                end
                Tasks(1:sz,i) = varargin{i};
            else
                error(['parameter ' num2str(i) ' is a ' class(varargin{i}) ', expected PATask instead.']);
            end
        end

    end

    NN = size(Tasks,2);
else
    error(['Unsupported argument of class ' class(varargin{1})]);
end

end

% Initialize used functions (check dependencies)
function [funcDatabase,allfuncs] = initFunctions(Tasks,task_config, NN, MM,sched)
v=version;
[vmaj rem] = strtok(v, '.');
vmaj = str2num(vmaj);
vmin = strtok(rem, '.');
vmin = str2num(vmin);
funcDatabase = [];

    function checkFunc(foo)
        if ischar(foo)
            return
        end
        if vmaj > 7 || vmin > 2
            try
                nargin(foo);
            catch err
                if strcmp(err.identifier,'MATLAB:nargin:isScript') == 1
                    error([char(foo) ' parameter is a script, expected a function']);
                else
                    throw(err);
                end
            end
        end
    end

    function sp = findScriptParams(obj, foo)
        % find the list of toolboxes used by the user function and give it as parameter to the script
        if ischar(foo)
            tblist = sched.findUsedToolboxes(obj, foo);
            if isempty(tblist)
                tblist = {'MATLAB'};
            end
        else
            foostr = func2str(foo);
            if foostr(2) ~= '('
                %if not anonymous function
                tblist = sched.findUsedToolboxes(obj, foostr);
                tblist = [{'MATLAB'} tblist];
            else
                % if func is an anonymous function, we can't find dependencies
                tblist = {'MATLAB'};
            end
        end
        sp = javaArray('java.lang.String',length(tblist));
        for II=1:length(tblist)
            sp(II) = java.lang.String(tblist{II});
        end

    end

for i=1:NN
    for j=1:MM
        if isa(Tasks(j,i).Func,'function_handle') || ischar(Tasks(j,i).Func)
            checkFunc(Tasks(j,i).Func);
            if (~isempty(Tasks(j,i).Object))
                allfuncs(i,j).o = Tasks(j,i).Object;
                allfuncs(i,j).f = Tasks(j,i).Func;
                allfuncs(i,j).hash = ['H' char(org.ow2.proactive.scheduler.ext.common.util.IOTools.generateHash(strcat(class(Tasks(j,i).Object), '_', Tasks(j,i).Func)))]; 
            else
                allfuncs(i,j).o = [];
                allfuncs(i,j).f = Tasks(j,i).Func;
                allfuncs(i,j).hash = ['H' char(org.ow2.proactive.scheduler.ext.common.util.IOTools.generateHash(func2str(Tasks(j,i).Func)))];
            end
            
            % allfuncs(i,j).s = strfunc;
            if ~isfield(funcDatabase, allfuncs(i,j).hash)
                % find the list of toolboxes used by the user function and give it as parameter to the script
                sp = findScriptParams(allfuncs(i,j).o, allfuncs(i,j).f);
                funcDatabase.(allfuncs(i,j).hash).sp = sp;
            else
                sp = funcDatabase.(allfuncs(i,j).hash).sp;
            end
            task_config(i,j).setToolboxesUsed(sp);
        else
            error(['Parameter ' num2str(i) ',' num2str(j)  ' has no function definition.']);
        end

    end
end
end



% Initilize directories used
function [globalFilesToClean,taskFilesToClean,pa_dir,curr_dir,fs,subdir, solveid] = initDirectories(opt,solve_config,NN,solveid)

fs = filesep;

curr_dir = pwd;
curr_dir_java = java.io.File(curr_dir);
if ~curr_dir_java.canWrite()
    error('Current Directory should have write access rights');
end

subdir = '.PAScheduler';

% get the local solve id from the task repository

trepository = org.ow2.proactive.scheduler.ext.matlab.client.embedded.MatlabTaskRepository.getInstance();
solveid = trepository.getNextLocalId();

if isnumeric(opt.CustomDataspaceURL) && isempty(opt.CustomDataspaceURL)
    if (~exist([curr_dir fs subdir],'dir'))
        mkdir(curr_dir,subdir);
    end
    if (~exist([curr_dir fs subdir fs num2str(solveid) 'm'],'dir'))
        mkdir([curr_dir fs subdir],[num2str(solveid) 'm']);
    end
    pa_dir = [curr_dir fs subdir fs num2str(solveid) 'm'];
else
    if isnumeric(opt.CustomDataspacePath) && isempty(opt.CustomDataspacePath)
        error('if CustomDataspaceURL is specified, CustomDataspacePath must be specified also');
    end
    if (~exist([opt.CustomDataspacePath fs subdir],'dir'))
        mkdir(opt.CustomDataspacePath,subdir);
    end
    if (~exist([opt.CustomDataspacePath fs subdir fs num2str(solveid) 'm'],'dir'))
        mkdir([opt.CustomDataspacePath fs subdir],[num2str(solveid) 'm']);
    end
    pa_dir = [opt.CustomDataspacePath fs subdir fs num2str(solveid) 'm'];
end


globalFilesToClean = {};
taskFilesToClean=cell(1,NN);
for i=1:NN
    taskFilesToClean{i}={};
end

subdir = [subdir '/' num2str(solveid) 'm'];
solve_config.setJobSubDirPath(subdir);
solve_config.setDirToClean(pa_dir);
end

% Initialize Data Spaces
function initDS(opt,sched, solve_config,curr_dir)

if isnumeric(opt.CustomDataspaceURL) && isempty(opt.CustomDataspaceURL)
    registry = sched.PAgetDataspaceRegistry();
    try 
        unreifiable = registry.createDataSpace(curr_dir);
    catch
        error('There was a problem contacting the middleman Java Virtual Machine, please reconnect using PAconnect');
    end
    pair = unreifiable.get();
    solve_config.setInputSpaceURL(pair.getX());
    solve_config.setOutputSpaceURL(pair.getY());

else
    solve_config.setOutputSpaceURL(opt.CustomDataspaceURL);
    solve_config.setInputSpaceURL(opt.CustomDataspaceURL);
end


end

% Initialize Remote Functions
function [keepaliveFunctionName,checktoolboxesFunctionName] = initRemoteFunctions(solve_config)
keepaliveFunctionName = 'keepalive_callback_fcn';
solve_config.setKeepaliveCallbackFunctionName(keepaliveFunctionName);
checktoolboxesFunctionName = 'checktoolboxes_start_and_hide_desktop';
solve_config.setChecktoolboxesFunctionName(checktoolboxesFunctionName);
end

% Initialize Global PASolve Config
function initSolveConfig(solve_config,opt,sched)
curr_dir = pwd();
solve_config.setJobName(opt.JobName);
solve_config.setJobDescription(opt.JobDescription);
solve_config.setDebug(opt.Debug);
lgin = sched.PAgetlogin();
solve_config.setLogin(lgin);
solve_config.setPriority(opt.Priority);
solve_config.setUseJobClassPath(opt.UseJobClassPath);
solve_config.setTransferEnv(opt.TransferEnv);
solve_config.setMatFileOptions(opt.TransferMatFileOptions);
solve_config.setLicenseSaverURL(opt.LicenseSaverURL);
solve_config.setFork(opt.Fork);
solve_config.setRunAsMe(opt.RunAsMe);
solve_config.setToolboxPath(opt.MatSciDir);
solve_config.setSharedPushPublicUrl(opt.SharedPushPublicUrl);
solve_config.setSharedPullPublicUrl(opt.SharedPullPublicUrl);
solve_config.setSharedPushPrivateUrl(opt.SharedPushPrivateUrl);
solve_config.setSharedPullPrivateUrl(opt.SharedPullPrivateUrl);
solve_config.setSharedAutomaticTransfer(opt.SharedAutomaticTransfer);
solve_config.setJobDirectoryFullPath(curr_dir);
solve_config.setNbExecutions(opt.NbTaskExecution);   
solve_config.setWindowsStartupOptionsAsString(opt.WindowsStartupOptions);
solve_config.setLinuxStartupOptionsAsString(opt.LinuxStartupOptions);

solve_config.setInputSpaceName('MatlabInputSpace');
solve_config.setOutputSpaceName('MatlabOutputSpace');

solve_config.setVersionPref(opt.VersionPref);
solve_config.setVersionRejAsString(opt.VersionRej);
solve_config.setVersionMin(opt.VersionMin);
solve_config.setVersionMax(opt.VersionMax);
solve_config.setVersionArch(opt.VersionArch);
solve_config.setForceMatSciSearch(opt.ForceMatlabSearch);
solve_config.setFindMatSciScriptUrl(opt.FindMatlabScript);
solve_config.setCheckLicenceScriptUrl(opt.MatlabReservationScript);
if ischar(opt.CustomScript)
    select = opt.CustomScript;
    try
        java.net.URL(select);
        ok = true;
    catch ME
        ok = false;
    end

    if ~ok
        solve_config.setCustomScriptUrl(['file:' select]);
    else
        solve_config.setCustomScriptUrl(select);
    end
    solve_config.setCustomScriptStatic(opt.CustomScriptStatic);
    solve_config.setCustomScriptParams(opt.CustomScriptParams);
end
solve_config.setFindMatSciScriptStatic(opt.FindMatSciScriptStatic);
solve_config.setUseMatlabControl(opt.UseMatlabControl);
solve_config.setWorkerTimeoutStart(opt.WorkerTimeoutStart);

worker_lib_dir = opt.WorkerJarsDir;
if ~exist(worker_lib_dir,'dir')
    error(['PAconnect::cannot find directory defined in option WorkerJarsDir : ' worker_lib_dir]);
end
dir_content = dir([worker_lib_dir filesep '*.jar']);
for i=1:length(dir_content)
    jarFullPath = [worker_lib_dir filesep dir_content(i).name];    
    solve_config.addWorkerJar(jarFullPath);
end

end



% Initialize task config for Transfer source (zip function used)
function [funcDatabase,taskFilesToClean] = initTransferSource(opt, fs, solveid, funcDatabase, sched, allfuncs, NN, MM,Tasks,keepaliveFunctionName,checktoolboxesFunctionName,taskFilesToClean,task_config,pa_dir,subdir, local_names,local_types, global_names, global_types)
sourceZipBaseName = ['MatlabPAsolveSrc_' num2str(solveid)];


    function  [zFN zFP]=buildZiplist(obj, foo, hash, ind,envziplist,paramziplist)
        if ~isfield(funcDatabase, hash) || ~isfield(funcDatabase.(hash),'dep')
            if ~isempty(obj)
                [mfiles classdirs] = sched.findDependency(obj, foo);
            else
                [mfiles classdirs] = sched.findDependency(obj, func2str(foo));
            end
            funcDatabase.(hash).dep.mfiles = mfiles;
            funcDatabase.(hash).dep.classdirs = classdirs;

        else
            mfiles = funcDatabase.(hash).dep.mfiles;
            classdirs = funcDatabase.(hash).dep.classdirs;
        end

        z = union(mfiles, classdirs);
        z=union(z,envziplist);
        z=union(z,paramziplist);
        [pasolvepath, pasolvename, pasolveext] = fileparts(mfilename('fullpath'));

        keepalive_cb_path = [pasolvepath, fs, 'Utils', fs, keepaliveFunctionName, '.m'];
        checktoolboxes_fn_path = [pasolvepath, fs, 'Utils', fs, checktoolboxesFunctionName, '.m'];
        z=union(z, {keepalive_cb_path,checktoolboxes_fn_path});
        bigstr = '';
        for kk = 1:length(z)
            bigstr = [bigstr z{kk}];
        end
        hashsource = char(org.ow2.proactive.scheduler.ext.common.util.IOTools.generateHash(bigstr));
        zFN = [sourceZipBaseName '_' hashsource '.zip'];
        zFP = [pa_dir fs zFN];
        if ~exist(zFP, 'file')
            zip(zFP, z);
        end
        %         if length(z) > 0

        %         else
        %             % Dummy code in case there is no file to zip
        %             zFP = [pa_dir fs zFN];
        %             zip(zFP, {[mfilename('fullpath') '.m']});
        %             h = char(org.ow2.proactive.scheduler.ext.common.util.IOTools.generateHash(zFP));
        %         end
    end



stdclasses = {'logical','char','int8','uint8','int16','uint16','int32','uint32','int64','uint64','single','double','cell','struct','function_handle','com','java','interface'};
envziplist={};
if opt.TransferEnv
    global_names_mod = cellfun(@(x) [' ' x] ,global_names,'UniformOutput',false);
    global_names_str = cell2mat(global_names_mod);
    if length(global_names_str) > 0
        eval(['global' global_names_str]);
    end

    for i=1:(length(local_names)+length(global_names))
        if i <= length(local_names)
            c = local_types{i};
            ok = ismember(c, stdclasses);
        else
            c = global_types{i-length(local_names)};
            ok = ismember(c, stdclasses);
        end
        if ok
        else
            if ~isfield(funcDatabase, c) || ~isfield(funcDatabase.(c),'dep')
                [envmfiles envclassdirs] = sched.findDependency([], c);
                funcDatabase.(c).dep.mfiles = envmfiles;
                funcDatabase.(c).dep.classdirs = envclassdirs;

            else
                envmfiles = funcDatabase.(c).dep.mfiles;
                envclassdirs = funcDatabase.(c).dep.classdirs;
            end
            envziplist = union(envziplist,envmfiles);
            envziplist = union(envziplist,envclassdirs);
        end
    end
end

for i=1:NN
    for j=1:MM
        paramziplist={};

        argi = Tasks(j,i).Params;
        for k=1:length(argi)
            c=class(argi{k});
            if ismember(c, stdclasses) || iscom(argi{k}) || isjava(argi{k}) || isinterface(argi{k})
            else
                if ~isfield(funcDatabase, c) || ~isfield(funcDatabase.(c),'dep')
                    [parammfiles paramclassdirs] = sched.findDependency([], c);
                    funcDatabase.(c).dep.mfiles = parammfiles;
                    funcDatabase.(c).dep.classdirs = paramclassdirs;

                else
                    parammfiles = funcDatabase.(c).dep.mfiles;
                    paramclassdirs = funcDatabase.(c).dep.classdirs;
                end

                paramziplist = union(paramziplist, parammfiles);
                paramziplist = union(paramziplist, paramclassdirs);
            end
        end
        [zFN zFP]=buildZiplist(allfuncs(i,j).o, allfuncs(i,j).f, allfuncs(i,j).hash, [i j], envziplist, paramziplist);
        sourceZip = org.ow2.proactive.scheduler.ext.matsci.common.data.PASolveZippedFile(subdir, zFN);
        task_config(i,j).addSourceFile(sourceZip);
        % taskFilesToClean{i}=[taskFilesToClean{i} {zFP}];

    end
end
end


% Initialize Task Config Input Files
function [taskFilesToClean] = initInputFiles(NN,MM,Tasks,opt,fs,taskFilesToClean,task_config)
for i=1:NN
    for j=1:MM
        ilen = length(Tasks(j,i).InputFiles);
        if ilen > 0
            filelist = Tasks(j,i).InputFiles;
            for k=1:ilen
                aFile = filelist(k);
                if isAbsolute(aFile.Path)
                    error([aFile.Path ' is an absolute pathname, please use a relative pathname that is a decendant of the current directory.']);
                end
                ifstr = java.lang.String(aFile.Path);
                dss = org.ow2.proactive.scheduler.ext.matsci.common.data.DSSource.getSpace(aFile.Space);
                task_config(i,j).addInputFile(ifstr, dss);
            end
        end
    end

end
end

% Initialize Task Config Output Files
function [taskFilesToClean] = initOutputFiles(NN,MM,Tasks,opt,subdir,pa_dir,taskFilesToClean,task_config)
outputFilesLists = [];
outputFiles = [];

for i=1:NN
    for j=1:MM
        filelist = Tasks(j,i).OutputFiles;
        noutput = length(filelist);
        if noutput > 0

            %outputZipName = [outputZipBaseName indTofile([i j]) '.zip'];
            %outputZipPath = [subdir '/' outputZipName];

            for k=1:noutput
                aFile = filelist(k);
                if isAbsolute(aFile.Path)
                    error([aFile.Path ' is an absolute pathname, invalid for output files.']);
                end
                ifstr = java.lang.String(aFile.Path);
                dss = org.ow2.proactive.scheduler.ext.matsci.common.data.DSSource.getSpace(aFile.Space);
                task_config(i,j).addOutputFile(ifstr, dss);
                
            end            
        end
    end
end
end

% Initialize Task Config Input Parameters
function [input,main,taskFilesToClean,outVarFiles]=initParameters(solveid,NN,MM,Tasks,opt,taskFilesToClean,task_config,allfuncs,pa_dir,subdir,fs)

    function answer = topath(list)
        answer = list{1};
        for z = 2:length(list)
            answer = [answer ';' list{z}];
        end
    end

input = 'i=0;';

variableInFileBaseName = ['MatlabPAsolveVarIn_' num2str(solveid)];
variableOutFileBaseName = ['MatlabPAsolveVarOut_' num2str(solveid)];
curr_dir = pwd();
outVarFiles = cell(1,NN);
for i=1:NN
    for j=1:MM
        % Creating the input command
        % (We use this amazing contribution which converts (nearly) any variable
        % to an evaluatable string)
        argi = Tasks(j,i).Params;
        main ='';

        inVarFN = [variableInFileBaseName indToFile([i j]) '.mat'];
        outVarFN = [variableOutFileBaseName indToFile([i j]) '.mat'];
        inVarFP = [pa_dir fs inVarFN];
        outVarFP = [pa_dir fs outVarFN];
        % Creating input parameters mat files
        if length(argi) == 0
            in.in1=true;
        else
            in.obj = allfuncs(i,j).o;
            in.foo = allfuncs(i,j).f;
            for k=1:length(argi)
                in.(['in' num2str(k)]) = argi{k};
            end
        end
        if (ischar(opt.TransferMatFileOptions) && length(opt.TransferMatFileOptions) > 0)
            save(inVarFP,'-struct','in',opt.TransferMatFileOptions);
        else
            save(inVarFP,'-struct','in');
        end
        taskFilesToClean{i}=union(taskFilesToClean{i}, {inVarFP});
        % because of disconnected mode, the final out is handled
        % differently
        if j < MM
            taskFilesToClean{i}=union(taskFilesToClean{i}, {outVarFP});
        end
        task_config(i,j).setInputVariablesFile(subdir,inVarFN);
        task_config(i,j).setOutputVariablesFile(curr_dir,subdir,outVarFN);
        if j > 1 && Tasks(j,i).Compose
            task_config(i,j).setComposedInputVariablesFile(subdir,[variableOutFileBaseName indToFile([i j-1]) '.mat']);
        end

        % The last task in the chain contains the final output
        if j == MM
            outVarFiles{i} = outVarFP;
        end
        
        if ~opt.EnableFindDependencies
            if isempty(opt.MatlabPathList)
                pathlist = strsplit(userpath(),pathsep);
                for z = 1:length(pathlist)
                    main = [main 'addpath(''' pathlist{z} ''');'];
                end
            else
                for z = 1:length(opt.MatlabPathList)
                    main = [main 'addpath(''' opt.MatlabPathList{z} ''');'];
                end
            end
        end


        % Creating the rest of the command (evaluation of the user
        % function)
        if ~isempty(allfuncs(i,j).o)
            main = [main 'out = obj.' allfuncs(i,j).f '('];
        else
            main = [main 'out = foo('];
        end
        

        if j > 1 && length(argi) > 0 && Tasks(j,i).Compose
            main = [main 'in' ','];
        elseif j > 1 && Tasks(j,i).Compose
            main = [main 'in'];
        end
        if length(argi) > 0
            for k=1:length(argi)-1
                main = [main 'in' num2str(k) ','];
            end
            main = [main 'in' num2str(length(argi))];
        end
        if opt.Debug
            main = [main ')'];
        else
            main = [main ');'];
        end
        task_config(i,j).setInputScript(input);
        task_config(i,j).setMainScript(main);

    end
end
end

% Initialize other task config attributes
function initOtherTCAttributes(NN,MM, task_config, Tasks)
for i=1:NN
    for j=1:MM
        task_config(i,j).setDescription(Tasks(j,i).Description);
        if ischar(Tasks(j,i).SelectionScript)
            select = Tasks(j,i).SelectionScript;
            try
                java.net.URL(select);
                ok = true;
            catch ME
                ok = false;
            end

            if ~ok
                task_config(i,j).setCustomScriptUrl(['file:' select]);
            else
                task_config(i,j).setCustomScriptUrl(select);
            end
            task_config(i,j).setCustomScriptStatic(Tasks(j,i).Static);
            task_config(i,j).setCustomScriptParams(Tasks(j,i).ScriptParams);
        end
        if Tasks(j,i).NbNodes > 1
            if ~ischar(Tasks(j,i).Topology)
                error(['PAsolve::Topology is not defined in Task ' num2str(j) ',' num2str(i) ' with NbNodes > 1.']);
            end
            task_config(i,j).setNbNodes(Tasks(j,i).NbNodes);
            task_config(i,j).setTopology(Tasks(j,i).Topology);
            task_config(i,j).setThresholdProximity(Tasks(j,i).ThresholdProximity);
        end
    end
end
end


function nm=indToFile(ind)
nm='';
if ind==-1
    return;
end
for JJ=ind
    nm=[nm '_' num2str(JJ)];
end
end

function ok=isAbsolute(file)
jf = java.io.File(file);
ok=jf.isAbsolute();
end

