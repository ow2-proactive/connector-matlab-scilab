function PAdisconnect()
    
    global ('PA_initialized', 'PA_connected', 'PA_solver')
    jimport org.ow2.proactive.scheduler.ext.scilab.client.embedded.util.ScilabJVMSpawnHelper;
    deployer = jinvoke(ScilabJVMSpawnHelper,'getInstance');
    PA_solver = jinvoke(deployer,'getScilabEnvironment');

    if ~exists('PA_initialized') | PA_initialized ~= 1
        PAinit();
    end    
    if ~exists('PA_connected') | PA_connected ~= 1 | ~jinvoke(PA_solver,'isLoggedIn')         
        error('This Matlab session is not connected to a Scheduler');
    end
    try
       jinvoke(PA_solver ,'disconnect'); 
       PA_connected = %f;
    catch                 
    end     
    disp("Disconnected")       
endfunction