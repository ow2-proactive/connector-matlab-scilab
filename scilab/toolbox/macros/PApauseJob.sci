function PApauseJob(jobid)
    global ('PA_solver');
    PAensureConnected();
    if or(type(jobid)==[1 5 8]) then
        jobid = string(jobid);
    end
    try
        txt = jinvoke(PA_solver,'pauseJob',jobid);
    catch
        PAensureConnected();
        txt = jinvoke(PA_solver,'pauseJob',jobid);
    end
    printf('%s\n',txt);
endfunction