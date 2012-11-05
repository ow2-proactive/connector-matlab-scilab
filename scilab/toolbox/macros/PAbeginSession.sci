function [tf] = PAbeginSession()
    global ('PA_solver');
    PAensureConnected();
    jimport org.ow2.proactive.scheduler.ext.scilab.client.embedded.ScilabTaskRepository;   
    repository = jinvoke(ScilabTaskRepository,'getInstance');
    jinvoke(repository, 'beginSession');
    jremove(repository);
    pair = jinvoke(PA_solver, 'beginSession');
    tf = jinvoke(pair,'getX');
    message = jinvoke(pair,'getY');
    printf('%s\n',message);
    jremove(pair);
endfunction