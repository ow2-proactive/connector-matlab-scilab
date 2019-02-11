function tf=PAisAwaited(l)
    global('PA_solver')
    if typeof(l) == 'PAResL' then
        m = size(l.matrix,2);
        R=l.matrix{1};
        jobid = R.jobid;
        jimport java.util.ArrayList;
        taskids = jnewInstance(ArrayList);
        for i=1:m
            R = l.matrix{i};
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
            tf(i)=answers(i);
        end        
        jremove(answers);
        jremove(unrei);
        jremove(taskids);
        jremove(ArrayList);
    elseif typeof(l) == 'PAResult' then
        R = l;
        jobid = R.jobid;
        jimport java.util.ArrayList;
        taskids = jnewInstance(ArrayList);
        jinvoke(taskids,'add',R.taskid);

        try
           unrei = jinvoke(PA_solver,'areAwaited',jobid, taskids);
        catch
           PAensureConnected();
           unrei = jinvoke(PA_solver,'areAwaited',jobid, taskids);
        end
        answers = jinvoke(unrei,'get');

        tf=jinvoke(answers,'get', 0);

        jremove(answers);
        jremove(unrei);
        jremove(taskids);
        jremove(ArrayList);
    else
        error('Expected argument of type PAResL, received '+typeof(l))
    end
endfunction
