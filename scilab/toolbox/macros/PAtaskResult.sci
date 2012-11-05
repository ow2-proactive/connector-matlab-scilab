function PAtaskResult(jobid,taskname)
    global ('PA_solver');
    PAensureConnected();
    if or(type(jobid)==[1 5 8]) then
        jobid = string(jobid);
    end

    try
        txt = jinvoke(PA_solver,'taskResult',jobid,taskname);
    catch
        PAensureConnected();
        txt = jinvoke(PA_solver,'taskResult',jobid,taskname);
    end
    printf('%s\n',txt);   
endfunction