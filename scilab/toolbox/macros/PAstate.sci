function [] = PAstate()
    global ('PA_solver');
    PAensureConnected();
    
    txt = jinvoke(PA_solver, 'schedulerState');    
    pa_printf('%s\n',txt);
endfunction