function opts = PAoptions(varargin)

function [n,key,value]=getkeyvalue(fid)
    key = []
    value = []
    n = -1
    line = getline(fid);
    start=[]
    if ~isempty(line) then
        [start]=strindex(line,'%%')
    end
    while ~isempty(line) & ~isempty(start)
        line = getline(fid);
        if ~isempty(line) then
            [start]=strindex(line,'%%')
        end
    end
    if ~isempty(line) then
         pos = strcspn(line,'=')
         key=stripblanks(part(line,1:pos), %t)
         res = part(line,pos+2:length(line))
          value=stripblanks(res, %t)
          n=1
    end

endfunction

function [line]=getline(fid)
    eof = meof(fid);
    line=[];
    while ~eof & isempty(line)
        line=mgetl(fid,1);
        if ~isempty(line) then
            line=stripblanks(line, %t)
        end
        eof = meof(fid);
    end
endfunction

function y=logcheck(x)
    if islogical(x) then
        y=%t
    elseif isnumeric(x) then
        y=((x == 0)|(x == 1))
    elseif ischar(x) then
        y=ismember(x,{'true','false'})
    else
        y=%f
    end
endfunction

function cl=stringtocell(x)
    cl=cell();
    i=1;
    remain=x;
    if iscell(x) then
        cl = x;
        return
    end
    goon=%t;
    str = strtok(remain, ',; ');
    while goon
        if isempty(str) | length(str) == 0 then
            goon=%f;
        else
            cl(i).entries=str;
            i=i+1;
        end
        str = strtok(',; ');
    end
endfunction

function cl=stringtocell2(x)
    cl=cell();
    i=1;
    remain=x;
    if iscell(x) then
        cl = x;
        return
    end
    goon=%t;
    str = strtok(remain, ' ');
    while goon
        if isempty(str) | length(str) == 0 then
            goon=%f;
        else
            cl(i).entries=str;
            i=i+1;
        end
        str = strtok(' ');
    end
endfunction

function cl=stringtolist(x)
    cl=list();
    i=1;
    remain=x;
    if iscell(x) then
        cl = x;
        return
    end
    goon=%t;
    str = strtok(remain, ',; ');
    while goon
        if isempty(str) | length(str) == 0 then
            goon=%f;
        else
            cl($+1)=str;
            i=i+1;
        end
        str = strtok(',; ');
    end
endfunction



    global ('proactive_pa_options', 'PA_matsci_dir')
    if  argn(2) == 0 & ~isempty(proactive_pa_options) then
        opts = proactive_pa_options; 
        return;
    elseif pmodulo(argn(2),2) ~= 0
        error(strcat(['Wrong number of arguments : ' string(argn(2))]));
    end

    deff ("y=isnumeric(x)","y=or(type(x)==[1,5,8])","n");
    deff ("y=islogical(x)","y=or(type(x)==[4,6])","n");
    deff ("y=ischar(x)","y=type(x)==10","n");
    deff ("y=isvoid(x)","if isnumeric(x), y=isempty(x), elseif ischar(x), y=isempty(x), else y=%f, end", "n");
    deff ("y=ismember(a,l)","y=(or(a==l))","n");
    //  deff ("y=logcheck(x)","if islogical(x), y=%t, elseif isnumeric(x), y=((x == 0)|(x == 1))elseif ischar(x), y=ismember(x,{''true'',''false''}), else y=%f, end","n");
    deff ("y=versioncheck(x)","if isnumeric(x), y=isempty(x), elseif ischar(x), y=~isempty(regexp(x, ''/^[1-9][0-9]*(\.[0-9]+)*$/'')), else y=%f, end","n");
    deff ("y=versionlistcheck(x)","if isnumeric(x), y=isempty(x), elseif ischar(x), y=~isempty(regexp(x, ''/^([1-9][0-9]*(\.[0-9]+)*[ ;,]+)*[1-9][0-9]*(\.[0-9]+)*$/'')), else y=%f, end","n");
    deff ("y=jarlistcheck(x)","if ischar(x), y=~isempty(regexp(x, ''/^([\w\-\.]+\.jar[ ;,]+)*[\w\-\.]+\.jar$/'')), else y=%f ,end","n");
    deff ("y=listcheck(x)","if ischar(x), y=~isempty(regexp(x, ''/^([^ ;,]+[ ;,]+)*[^ ;,]+$/'')), else y=%f ,end","n");
    deff ("y=listcheck2(x)","if isvoid(x),y=%t, elseif ischar(x), y=~isempty(regexp(x, ''/^([^ ]+[ ]+)*[^ ]+$/'')), else y=%f ,end","n");

    listtrans = stringtolist;

    deff ("y=urlcheck(x)","if isnumeric(x), y=isempty(x), else y=ischar(x), end","n");
    deff ("y=charornull(x)","if isnumeric(x), y=isempty(x), else y=ischar(x), end","n");   

    deff("y=isstrictpositiveint(x)","ss=evstr(x),if isnumeric(ss) & sum(length(ss))==1 & ss > 0 & floor(ss) == ss, y=%t, else y=%f, end","n");  


    v = getversion();
    v = strsubst(v,'scilab-','');
    majt = strtok(v,'.');
    mint = strtok('.');
    maint = strtok('.');  

    fs = filesep();
    tmp_dir = system_getproperty('java.io.tmpdir');
    home_dir = system_getproperty('user.home');
    deff ("y=logtrans(x)","if islogical(x), y=x, elseif ischar(x), y=( x == ''on'' | x == ''true''), elseif isnumeric(x), y=(x==1), end","n");
    deff ("y=variabletrans(x)","y=strsubst(strsubst(strsubst(x, ''$MATSCI$'', PA_matsci_dir),''$TMP$'',tmp_dir), ''$HOME$'', home_dir)","n");
    deff ("y=scripttrans(x)","if ischar(x), y=strcat([''file:'', strsubst(variabletrans(x), ''\'', ''/'')]),else y=x, end","n");
    deff ("y=conftrans(x)","y=strsubst(variabletrans(x),''/'',filesep())","n");

    deff ("y=ischarornull(x)","if isnumeric(x), y=isempty(x), else y=ischar(x), end","n");
    deff ("y=charornum(x)","y=or(type(x)==[1,5,8,10])","n");
    deff ("y=charornumtrans(x)","if isnumeric(x), y=x, else y=evstr(x), end","n");

    deff ("y=prioritycheck(x)","if ischar(x), y=ismember(x,{''Idle'', ''Lowest'', ''Low'', ''Normal'', ''High'', ''Highest''}), else y=%f, end","n")
     deff ("y=versionarchcheck(x)","if ischar(x), y=ismember(x,{''any'', ''32'', ''64''}), else y=%f, end","n")


    deff ("y=id(x)","y=x","n");

    j=1;
    inputs(j).name = 'JobName';
    inputs(j).default = 'Scilab';
    inputs(j).check = 'ischar';
    inputs(j).trans = 'id';
    j=j+1;
    inputs(j).name = 'JobDescription';
    inputs(j).default = 'Set of parallel Scilab tasks';
    inputs(j).check = 'ischar';
    inputs(j).trans = 'id';
    j=j+1;
    inputs(j).name = 'Debug';
    inputs(j).default = %f;
    inputs(j).check = 'logcheck';
    inputs(j).trans = 'logtrans';
    j=j+1;
    inputs(j).name = 'Fork';
    inputs(j).default = %f;
    inputs(j).check = 'logcheck';
    inputs(j).trans = 'logtrans';
    j=j+1;
    inputs(j).name = 'RunAsMe';
    inputs(j).default = %f;
    inputs(j).check = 'logcheck';
    inputs(j).trans = 'logtrans';
    j=j+1;
    inputs(j).name = 'SharedPushPublicUrl';
    inputs(j).default = [];
    inputs(j).check = 'urlcheck';
    inputs(j).trans = 'id';
    j=j+1;
    inputs(j).name = 'SharedPullPublicUrl';
    inputs(j).default = [];
    inputs(j).check = 'urlcheck';
    inputs(j).trans = 'id';
    j=j+1;
    inputs(j).name = 'SharedPushPrivateUrl';
    inputs(j).default = [];
    inputs(j).check = 'urlcheck';
    inputs(j).trans = 'id';
    j=j+1;
    inputs(j).name = 'SharedPullPrivateUrl';
    inputs(j).default = [];
    inputs(j).check = 'urlcheck';
    inputs(j).trans = 'id';
    j=j+1;
    inputs(j).name = 'SharedAutomaticTransfer';
    inputs(j).default = %t;
    inputs(j).check = 'logcheck';
    inputs(j).trans = 'logtrans';
    j=j+1;
    inputs(j).name = 'RemoveJobAfterRetrieve';
    inputs(j).default = %f;
    inputs(j).check = 'logcheck';
    inputs(j).trans = 'logtrans';
    j=j+1;
    inputs(j).name = 'NbTaskExecution';
    inputs(j).default = 2;
    inputs(j).check = 'isstrictpositiveint';
    inputs(j).trans = 'evstr';
    j=j+1;
    inputs(j).name = 'TransferEnv';
    inputs(j).default = %f;
    inputs(j).check = 'logcheck';
    inputs(j).trans = 'logtrans';    
    j=j+1;
    inputs(j).name = 'EnvExcludeList';
    inputs(j).default = 'demolist;scicos_pal;%scicos_menu;version;compiled;profilable;ans;called;%scicos_short;%helps;%helps_modules;MSDOS;who_user;%scicos_display_mode;%scicos_help';
    inputs(j).check = 'listcheck';
    inputs(j).trans = 'stringtocell';
    j=j+1;
    inputs(j).name = 'EnvExcludeTypeList';
    inputs(j).default = 'library;_Jobj;function';
    inputs(j).check = 'listcheck';
    inputs(j).trans = 'stringtocell';
    j=j+1;
    inputs(j).name = 'CustomDataspaceURL';
    inputs(j).default = [];
    inputs(j).check = 'urlcheck';
    inputs(j).trans = 'id';
    j=j+1;
    inputs(j).name = 'CustomDataspacePath';
    inputs(j).default = [];
    inputs(j).check = 'charornull';
    inputs(j).trans = 'id';
    j=j+1;
    inputs(j).name = 'VersionPref';
    inputs(j).default = strcat([majt, '.', mint, '.', maint]);
    inputs(j).check = 'versioncheck';
    inputs(j).trans = 'id';
    j=j+1;
    inputs(j).name = 'VersionRej';
    inputs(j).default = [];
    inputs(j).check = 'versionlistcheck';
    inputs(j).trans = 'id';
    j=j+1;
    inputs(j).name = 'VersionMin';
    inputs(j).default = [];
    inputs(j).check = 'versioncheck';
    inputs(j).trans = 'id';
    j=j+1;
    inputs(j).name = 'VersionMax';
    inputs(j).default = [];
    inputs(j).check = 'versioncheck';
    inputs(j).trans = 'id';
    j=j+1;
    inputs(j).name = 'VersionArch';
    inputs(j).default = 'any';
    inputs(j).check = 'versionarchcheck';
    inputs(j).trans = 'id';
    j=j+1;
    inputs(j).name = 'ForceScilabSearch';
    inputs(j).default = %f;
    inputs(j).check = 'logcheck';
    inputs(j).trans = 'logtrans';
    j=j+1;
    inputs(j).name = 'FindScilabScript';
    inputs(j).default = '$MATSCI$' + fs +'script' + 'file_scilab_finder.rb';
    inputs(j).check = 'ischar';
    inputs(j).trans = 'scripttrans';
    j=j+1;
    inputs(j).name = 'CustomScript';
    inputs(j).default = [];
    inputs(j).check = 'ischarornull';
    inputs(j).trans = 'scripttrans';
    j=j+1;
    inputs(j).name = 'CustomScriptStatic';
    inputs(j).default = %f;
    inputs(j).check = 'logcheck';
    inputs(j).trans = 'logtrans';
    j=j+1;
    inputs(j).name = 'CustomScriptParams';
    inputs(j).default = [];
    inputs(j).check = 'ischarornull';
    inputs(j).trans = 'id';
    j=j+1;
    inputs(j).name = 'FindMatSciScriptStatic';
    inputs(j).default = %t;
    inputs(j).check = 'logcheck';
    inputs(j).trans = 'logtrans';
    j=j+1;
    inputs(j).name = 'Priority';
    inputs(j).default = 'Normal';
    inputs(j).check = 'prioritycheck';
    inputs(j).trans = 'id';
    j=j+1;
    inputs(j).name = 'UseJobClassPath';
    inputs(j).default = %t;
    inputs(j).check = 'logcheck';
    inputs(j).trans = 'logtrans';
    j=j+1;
    inputs(j).name = 'WindowsStartupOptions';
    inputs(j).default = '-nw';
    inputs(j).check = 'ischarornull';
    inputs(j).trans = 'id';
    j=j+1;
    inputs(j).name = 'LinuxStartupOptions';
    inputs(j).default = '-nw';
    inputs(j).check = 'ischarornull';
    inputs(j).trans = 'id';
    j=j+1;
    //    inputs(j).name = 'ZipInputFiles';
    //    inputs(j).default = %f;
    //    inputs(j).check = 'logcheck';
    //    inputs(j).trans = 'logtrans';
    //    j=j+1;
    //    inputs(j).name = 'ZipOutputFiles';
    //    inputs(j).default = %f;
    //    inputs(j).check = 'logcheck';
    //    inputs(j).trans = 'logtrans';
    //    j=j+1;
    inputs(j).name = 'PathJars';
    inputs(j).default = '$MATSCI$'+ fs +'lib';
    inputs(j).check = 'ischar';
    inputs(j).trans = 'conftrans';
    j=j+1;

    inputs(j).name = 'EmbeddedJars';
    inputs(j).default = 'ProActive_Matlab_Scilab_Embedded.jar;jdbm-2_4.jar';
    inputs(j).check = 'jarlistcheck';
    inputs(j).trans = 'stringtocell';
    j=j+1;
    inputs(j).name = 'ProActiveJars';
    inputs(j).default = 'jruby.jar;jruby-engine.jar;jython.jar;jython-engine.jar;ProActive.jar;ProActive_Scheduler-core.jar;ProActive_SRM-common.jar;ProActive_Matlab_Scilab.jar';
    inputs(j).check = 'jarlistcheck';
    inputs(j).trans = 'stringtocell';
    j=j+1;
    inputs(j).name = 'WorkerJars';
    inputs(j).default = 'ProActive_Matlab_Scilab.jar;matlabcontrol-3.1.0.jar;ProActive_LicenseSaver-1.0.0-api.jar';
    inputs(j).check = 'jarlistcheck';
    inputs(j).trans = 'stringtocell';
    j=j+1;
    inputs(j).name = 'ProActiveConfiguration';
    inputs(j).default = '$MATSCI$'+ fs + 'config' + fs +'ScilabProActiveConfiguration.xml';
    inputs(j).check = 'ischar';
    inputs(j).trans = 'conftrans'
    j=j+1;
    inputs(j).name = 'Log4JConfiguration';
    inputs(j).default = '$MATSCI$'+ fs + 'config' + fs + 'log4j-client';
    inputs(j).check = 'ischar';
    inputs(j).trans = 'conftrans';
    j=j+1;
    inputs(j).name = 'SecurityFile';
    inputs(j).default = '$MATSCI$'+ fs + 'config' + fs +  'security.java.policy-client';
    inputs(j).check = 'ischar';
    inputs(j).trans = 'conftrans';
    j=j+1;
    inputs(j).name = 'MatSciDir';
    inputs(j).default = PA_matsci_dir;
    inputs(j).check = 'ischar';
    inputs(j).trans = 'id';
    j=j+1;
    inputs(j).name = 'EnableDisconnectedPopup';
    inputs(j).default = %t;
    inputs(j).check = 'logcheck';
    inputs(j).trans = 'logtrans';
    j=j+1;
    inputs(j).name = 'RmiPort';
    inputs(j).default = 1111;
    inputs(j).check = 'charornum';
    inputs(j).trans = 'charornumtrans';
    j=j+1;
    inputs(j).name = 'JvmArguments';
    inputs(j).default = [];
    inputs(j).check = 'listcheck2';
    inputs(j).trans = 'stringtocell2';
    j=j+1;
    inputs(j).name = 'JvmTimeout';
    inputs(j).default = 1200;
    inputs(j).check = 'charornum';
    inputs(j).trans = 'charornumtrans';
    j=j+1;
    inputs(j).name = 'WorkerTimeoutStart';
    inputs(j).default = 6000;
    inputs(j).check = 'charornum';
    inputs(j).trans = 'charornumtrans';

    inlength = j; 

    // Parsing option file
    if isempty(proactive_pa_options) then 
        userdir = home_dir;
        optionpath = userdir + fs + '.scilab' + fs + 'PAoptions.ini'; 
        if isfile(optionpath) then
            [fid, ferr] = mopen(optionpath, 'r'); 
        else
            optionpath = fullfile(PA_matsci_dir, 'config', 'PAoptions.ini');
            if ~isfile(optionpath) then
                error(strcat(['Can''t locate options file at ""';optionpath;'"".']));
            end
            [fid, ferr] = mopen(optionpath, 'r');
        end    
        try

            [n,key, value] = getkeyvalue(fid);

            while n~=-1
                deblanked = stripblanks(key(1), %t);                                
                for j=1:inlength                          
                    if strcmp(deblanked,inputs(j).name) == 0 then
                        chk = inputs(j).check; 
                        tf = evstr(strcat([chk, '(value)']));                               
                        if ~tf then
                            disp(value)
                            error('Parse error when loading option file ' + optionpath + ', option ' + deblanked + ' doesn''t satisfy check '+ chk );
                        end
                        transfunc = inputs(j).trans;
                        def = evstr(strcat([transfunc, '(value)']))
                        proactive_pa_options(inputs(j).name) =  def;                   
                    end
                end
                [n,key, value] = getkeyvalue(fid);
            end
        catch 
            mclose(fid);
            [str2,n2,line2,func2]=lasterror(%t);printf('!-- error %i %s at line %i of function %s',n2,str2,line2,func2);        
            error('Error while reading configuration file at ' + optionpath);
        end  
        mclose(fid);
    end    

    for i = 1:inlength
        default = %t;
        transfunc = inputs(i).trans;
        Parameter = inputs(i).default;
        for j= 1:argn(2)/2
            optionName = varargin(2*(j-1)+1);
            value = varargin(2*j);

            if inputs(i).name == optionName then
                chk = inputs(i).check;
                tf = evstr(strcat([chk, '(value)']));
                if ~tf then
                    disp(value)
                    error('Argument '+ optionName+ ' doesn''t satisfy check '+ chk );
                end
                default = %f;                    
                Parameter = evstr(strcat([transfunc, '(value)']));                                       
            end
        end

        if ~default | ~(isstruct(proactive_pa_options) & isfield(proactive_pa_options,inputs(i).name)) then      
            proactive_pa_options(inputs(i).name) =  Parameter;        
        end
    end


    opts = proactive_pa_options;    

endfunction


