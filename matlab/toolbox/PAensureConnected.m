function PAensureConnected()
     sched = PAScheduler;
     solver = sched.PAgetsolver();
     if strcmp(class(solver),'double')
         error('Initial connection to the scheduler was not established, use PAconnect');
     end
     try
        solver.ensureConnection();
     catch
        % if there is an exception it means that we lost the RMI stubs
        deployer = org.ow2.proactive.scheduler.ext.matlab.client.embedded.util.MatlabJVMSpawnHelper.getInstance();
        deployer.updateAllStubs(true);
        solver = deployer.getMatlabEnvironment();
        sched.PAgetsolver(solver);
        registry = deployer.getDSRegistry();
        sched.PAgetDataspaceRegistry(registry);
        jvmint = deployer.getJvmInterface();
        sched.PAgetJVMInterface(jvmint);
        pause(2);
        PAensureConnected();

     end

end