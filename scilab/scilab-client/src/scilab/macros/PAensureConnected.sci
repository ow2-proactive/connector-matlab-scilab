function PAensureConnected()
     global ('PA_connected', 'PA_solver', 'PA_dsregistry', 'PA_jvminterface')
     if ~exists('PA_connected') | PA_connected ~= 1
           error('PAensureConnected::Connection to the scheduler must be first established, see PAconnect.');
           return;
     end
     try
        jinvoke(PA_solver ,'ensureConnection');
     catch
        // if there is an exception it means that we lost the RMI stubs
        jimport org.ow2.proactive.scheduler.ext.scilab.client.embedded.util.ScilabJVMSpawnHelper;
        deployer = jinvoke(ScilabJVMSpawnHelper,'getInstance');
        jinvoke(deployer, 'updateAllStubs', %t);
        PA_solver = jinvoke(deployer,'getScilabEnvironment');
        PA_dsregistry = jinvoke(deployer,'getDSRegistry');
        PA_jvminterface = jinvoke(deployer,'getJvmInterface');
        jremove(deployer);
        jremove(ScilabJVMSpawnHelper);
        sleep(2000);
        PAensureConnected();

     end

endfunction