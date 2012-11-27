function PAkillJob(jobid)
    global ('PA_solver');
    PAensureConnected();
    if or(type(jobid)==[1 5 8]) then
        jobid = string(jobid);
    end
    try
        txt = jinvoke(PA_solver,'killJob',jobid);
    catch
        PAensureConnected();
        txt = jinvoke(PA_solver,'killJob',jobid);
    end
    pa_printf('%s\n',txt);
endfunction