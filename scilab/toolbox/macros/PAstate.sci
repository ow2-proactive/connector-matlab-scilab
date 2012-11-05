function [] = PAstate()
    global ('PA_solver');
    PAensureConnected();
    
    txt = jinvoke(PA_solver, 'schedulerState');    
    printf('%s\n',txt);   
endfunction