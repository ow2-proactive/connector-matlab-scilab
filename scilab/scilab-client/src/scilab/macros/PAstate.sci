function [] = PAstate()
    global ('PA_solver');
    PAensureConnected();

    try
        txt = jinvoke(PA_solver, 'schedulerState');
    catch
        PAensureConnected();
        txt = jinvoke(PA_solver,'schedulerState');
    end
    pa_printf(txt + '\n');
endfunction