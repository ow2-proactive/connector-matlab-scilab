function jobs = PAconnect(uri,credpath)

    global ('PA_initialized', 'PA_connected','PA_solver', 'PA_scheduler_URI')
    jobs = [];
    initJavaStack();
    if ~exists('PA_initialized') | PA_initialized ~= 1
        PAinit();
        jimport java.lang.System;
        jimport org.scilab.modules.gui.utils.ScilabPrintStream;
        if jinvoke(ScilabPrintStream,'isAvailable') then
            inst = jinvoke(ScilabPrintStream,'getInstance');
            addJavaObj(inst);
            jinvoke(System, 'setOut',inst);
            jinvoke(ScilabPrintStream,'setRedirect',[]);            
        end
    end
    opt=PAoptions();
    

    if type(PA_solver) ~= 1 then
        isJVMdeployed = 1;
        isConnected = 0;        
        try
            isConnected = jinvoke(PA_solver,'isConnected');
        catch 
            isJVMdeployed = 0;
        end
    else        
        isJVMdeployed = 0;
        isConnected = 0;
    end
    if ~exists('uri')
        uri = [];
    end    
    if ~isempty(PA_scheduler_URI) & ~isempty(uri)  & PA_scheduler_URI ~= uri then
        // particular case when the scheduler uri changes, we redeploy everything
        isJVMdeployed = 0;
        isConnected = 0;
    else
        PA_scheduler_URI = uri;
    end
        
    if ~isJVMdeployed 
        deployJVM(opt,uri);  
    end 
    if ~isConnected then
        ok = jinvoke(PA_solver,'join', uri);
        if ~ok then
            logPath = jinvoke(PA_solver,'getLogFilePath');
            error('PAConnect::Error wile connecting to ' + uri +', detailed error message has been logged to ' + logPath);
        end
        jobs = dataspaces(opt);
        disp(strcat(['Connection successful to ', uri]));
    end
                                                   
    if ~jinvoke(PA_solver,'isLoggedIn')  
        if argn(2) == 2 then
            login(uri,credpath); 
        else
            login(uri); 
        end
        PA_connected = 1;                             
    else
        disp('Already connected');
    end    
    //jremove(ScilabSolver);
    clearJavaStack();
endfunction

function deployJVM(opt,uri)
    global ('PA_matsci_dir','PA_solver', 'PA_dsregistry', 'PA_jvminterface')
    jimport org.ow2.proactive.scheduler.ext.scilab.client.embedded.util.ScilabJVMSpawnHelper;
    jimport java.lang.String;    
    deployer = jinvoke(ScilabJVMSpawnHelper,'getInstance');
    addJavaObj(String);
    addJavaObj(ScilabJVMSpawnHelper);
    addJavaObj(deployer);

    home = getenv('JAVA_HOME',jre_path());  // this uses the JAVA_HOME env var or the jre used by scilab

    fs=filesep();
    if length(home) > 0
        if (getos() == "Windows") then
            jinvoke(deployer,'setJavaPath',home + fs + 'bin'+ fs +'java.exe');
        else
            jinvoke(deployer,'setJavaPath',home + fs + 'bin'+ fs +'java');
        end
        
    end

    matsci_dir = opt.MatSciDir;


    middleman_lib_dir = opt.MiddlemanJarsDir;
    if ~isdir(middleman_lib_dir) then
        error('PAconnect::cannot find directory defined in option MiddlemanJarsDir : ' + middleman_lib_dir);
    end

    fs=filesep();
    jarfiles = listfiles(middleman_lib_dir+fs+'*.jar');

    jarsjava = jarray('java.lang.String', size(jarfiles,1));
    addJavaObj(jarsjava);
    for i=1:size(jarfiles,1)
        jartmp = jnewInstance(String, jarfiles(i));
        jarsjava(i) = jartmp;
        addJavaObj(jartmp);
    end
    options = opt.JvmArguments;
    for i=1:size(options,1)
         jinvoke(deployer,'addJvmOption',options{i});
    end
    jinvoke(deployer,'setSchedulerURI', uri);
    jinvoke(deployer,'setMatSciDir', matsci_dir);
    jinvoke(deployer,'setDebug',opt.Debug);
    jinvoke(deployer,'setClasspathEntries',jarsjava);
    jinvoke(deployer,'setProActiveConfiguration',opt.ProActiveConfiguration);
    jinvoke(deployer,'setLog4JFile',opt.Log4JConfiguration);
    jinvoke(deployer,'setPolicyFile',opt.SecurityFile);
    jinvoke(deployer,'setClassName','org.ow2.proactive.scheduler.ext.scilab.middleman.ScilabMiddlemanDeployer');


    rmiport = opt.RmiPort;

    jinvoke(deployer,'setRmiPort',rmiport);

    // Scilab ISSUE: after having started the middleman jvm, scilab 6.0 crashs when compiling (deff, evstr,..)
    pair = jinvoke(deployer,'deployOrLookup');
    itfs = jinvoke(pair,'getX');
    port = jinvoke(pair,'getY');
    PAoptions('RmiPort',port);    
    PA_solver = jinvoke(deployer,'getScilabEnvironment');    

    PA_dsregistry = jinvoke(deployer,'getDSRegistry');

    PA_jvminterface = jinvoke(deployer,'getJvmInterface');
        
    
    disp('Connection to the middleman JVM has succeeded, now trying to connect to the scheduler');
endfunction

function login(uri,credpath)
    global ('PA_solver')
    jimport org.ow2.proactive.scheduler.ext.scilab.client.embedded.util.ScilabJVMSpawnHelper;
    // Logging in
    if ~jinvoke(PA_solver,'isLoggedIn') then
       
        if argn(2) == 2 
            try
                jinvoke(PA_solver,'login',credpath);
            catch        
                clearJavaStack();
                disp(lasterror());
                error('PAconnect::Authentication error');
            end
        elseif isempty(uri) & jinvoke(PA_solver,'hasCredentialsStored');
             try
                jinvoke(PA_solver,'login',[], [], []);
            catch        
                clearJavaStack();
                disp(lasterror());
                error('PAconnect::Authentication error');
            end
        else
            disp('Please enter login/password');
            deployer = jinvoke(ScilabJVMSpawnHelper,'getInstance');
            addJavaObj(deployer);
            jinvoke(deployer,'startLoginGUI');
            while ~jinvoke(PA_solver,'isLoggedIn') & jinvoke(deployer,'getNbAttempts') <= 3
                sleep(100);
            end
            if jinvoke(deployer,'getNbAttempts')  > 3 then
                clearJavaStack();
                error('PAconnect::Authentication error');
            end
        end
        disp('Login successful');
    end   

endfunction

function jobs = dataspaces(opt)
    global ('PA_dsregistry')    
    jinvoke(PA_dsregistry, 'init','ScilabInputSpace', 'ScilabOutputSpace', opt.Debug);
    jimport org.ow2.proactive.scheduler.ext.scilab.client.embedded.ScilabTaskRepository;
    repository = jinvoke(ScilabTaskRepository,'getInstance');
    notReceived = jinvoke(repository, 'notYetReceived');
    jobs = [];

    // Retrieve the number of uncomplete jobs before last scilab shutdown
    if type(notReceived) == 17
        notReceivedSize = jinvoke(notReceived, 'size');
    else // type == 10
        notReceivedSize = size(notReceived,'c');
    end

    if notReceivedSize ~= 0
          jobs = list();
          msg = 'The following jobs were uncomplete before last scilab shutdown : ';
          for j = 1:notReceivedSize
                  jid = notReceived(j);
                  msg = msg + ' ' + jid;
                  jobs(j) = jid;
          end
          disp(msg);
    end

    if type(notReceived) == 17
        jremove(notReceived);
    end
    jremove(repository);
    jremove(ScilabTaskRepository);
endfunction

function initJavaStack()
    global('JAVA_STACK')
    JAVA_STACK=list();    
endfunction

function addJavaObj(obj)
    global('JAVA_STACK')   
    JAVA_STACK($+1)=obj;    
endfunction

function clearJavaStack()
    global('JAVA_STACK')
    for i=length(JAVA_STACK):-1:1
        try
            jremove(JAVA_STACK(i));
        catch
        end
    end
    clearglobal('JAVA_STACK');
endfunction
