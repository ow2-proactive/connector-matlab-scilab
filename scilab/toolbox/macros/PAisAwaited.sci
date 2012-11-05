function tf=PAisAwaited(l)
    global('PA_solver')
    if typeof(l) == 'PAResL' then
        m = size(l.matrix,2);
        R=l.matrix(1).entries;
        jobid = R.jobid;
        jimport java.util.ArrayList;
        taskids = jnewInstance(ArrayList);
        for i=1:m
            R = l.matrix(i).entries;            
            jinvoke(taskids,'add',R.taskid); 
        end
        try
            unrei = jinvoke(PA_solver,'areAwaited',jobid, taskids);
        catch
            PAensureConnected();
            unrei = jinvoke(PA_solver,'areAwaited',jobid, taskids);
        end
        answers = jinvoke(unrei,'get');
        for i=1:m
            tf(i)=jinvoke(answers,'get', i-1);
        end        
        jremove(answers);
        jremove(unrei);
        jremove(taskids);
    else
        error('Expected argument of type PAResL, received '+typeof(l))
    end
endfunction
