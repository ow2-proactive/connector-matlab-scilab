function [] = PAinit()

    global ('PA_initialized', 'PA_matsci_dir')

    jautoUnwrap(%t);    

    version = ver();
    if strtod(part(version(1,2),1)) < 5
        error('This toolkit cannot be run on a version of Scilab anterior to version 5');
    end
    // disp(PA_matsci_dir)
    if ~isdir(PA_matsci_dir)
        error('The path '''+PA_matsci_dir+''' doesn''t exist or is not a directory');
    end
    opt=PAoptions();
    dist_lib_dir = opt.PathJars;
    if ~isdir(dist_lib_dir) then
        error('PAinit::cannot find directory ' + dist_lib_dir);    
    end
     
        
    schedjar=fullfile(dist_lib_dir,opt.EmbeddedJars(1).entries); 
    if length(fileinfo(schedjar)) == 0 
        error('Can''t locate the scheduler jar at '''+schedjar);
    end    
    

    jimport java.io.File;
            

    sep=pathsep();

    // Add ProActive Scheduler to the scilab classpath
    initcp = javaclasspath();
    if strcmp(getos(),'Windows') == 0
        for i=1:size(initcp,1)
            initcp(i)=getlongpathname(strsubst(initcp(1),'%20',' '))
        end
    end

    listjars = opt.EmbeddedJars;

    cp = initcp;
    
    for i=1:size(listjars,1)        
        cp = [cp; fullfile(dist_lib_dir,listjars(i).entries)];
    end

    javaclasspath(cp);
        

    // Call the native JNI connection to the Scheduler classes
    //initEmbedded();

    PA_initialized = 1;

endfunction
