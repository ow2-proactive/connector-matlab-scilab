function [tf] = PAendSession()
    global ('PA_solver');
    PAensureConnected();
    jimport org.ow2.proactive.scheduler.ext.scilab.client.embedded.ScilabTaskRepository;   
    repository = jinvoke(ScilabTaskRepository,'getInstance');
    jinvoke(repository, 'endSession');
    jremove(repository);
    jremove(ScilabTaskRepository);
    pair = jinvoke(PA_solver, 'endSession');
    tf = jinvoke(pair,'getX');
    message = jinvoke(pair,'getY');
    mprintf('%s\n',message);
    jremove(pair);
endfunction