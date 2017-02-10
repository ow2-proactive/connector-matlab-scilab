function tf=PAisConnected()
    
    global ('PA_initialized', 'PA_connected','PA_solver')
    jimport org.ow2.proactive.scheduler.ext.scilab.client.embedded.util.ScilabJVMSpawnHelper;
    deployer = jinvoke(ScilabJVMSpawnHelper,'getInstance');
    PA_solver = jinvoke(deployer,'getScilabEnvironment');

    if ~exists('PA_initialized') | PA_initialized ~= 1
        PAinit();
    end       
    if ~exists('PA_connected') | PA_connected ~= 1
        tf = %f;        
        return;
    end   
    try
       tf = jinvoke(PA_solver ,'isConnected'); 
    catch 
        tf = %f;                
        return;
    end    
    if ~tf then        
        return;
    end
    tf = jinvoke(PA_solver ,'isLoggedIn');     
    return;
endfunction

