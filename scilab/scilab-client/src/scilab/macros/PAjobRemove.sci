function PAjobRemove(jobid)
    global ('PA_connected','PA_solver');
    if ~exists('PA_connected') | PA_connected ~= 1
        error('A connection to the ProActive scheduler must be established in order to use PAsolve, see PAconnect');
    end
    if or(type(jobid)==[1 5 8]) then
        jobid = string(jobid);
    end
    try
        txt = jinvoke(PA_solver,'jobRemove',jobid);
    catch
        PAensureConnected();
        txt = jinvoke(PA_solver,'jobRemove',jobid);
    end
    pa_printf('%s\n',txt);
endfunction