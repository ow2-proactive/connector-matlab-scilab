function [val_k,index]=PAwaitAny(l,timeout)
    global('PA_solver')
    val_k = [];
    if typeof(l) == 'PAResL' then         
        jimport java.util.ArrayList;    
        jimport java.lang.Integer;   
        R=l.matrix(1).entries;
        jobid = R.jobid;   
        taskids = jnewInstance(ArrayList);
        indList=list();    
        m = size(l.matrix,2);
        for i=1:m
            pares=l.matrix(i).entries;
            if ~jexists(pares.waited) then
                error('PAResult::object cleared');
            end
            if ~jinvoke(pares.waited,'get')
                jinvoke(taskids,'add',pares.taskid);
                indList($+1)=i;           
            end
        end
        if isempty(indList)
            error('All results have already been accessed');
        end

        if argn(2) == 2
            tout = jinvoke(Integer,'parseInt',string(timeout));
        else
            tout = jinvoke(Integer,'parseInt',string(-1));
        end
        try
            unrei = jinvoke(PA_solver,'waitAny',jobid, taskids,tout);
        catch
            PAensureConnected();
            unrei = jinvoke(PA_solver,'waitAny',jobid, taskids,tout);
        end
        pair = jinvoke(unrei,'get');
        ind = jinvoke(pair,'getY');

        j=indList(ind+1);
        index = j;
        pares=l.matrix(j).entries;
        RaL = jinvoke(pair,'getX');
        jinvoke(pares.RaL,'set', RaL);
        jinvoke(pares.waited,'set',%t);
        [val_k,err] = PAResult_PAwaitFor(pares);
        if ~isempty(err) then
            warning(err);
            warning('PAwaitAny:Error occured')
            val_k = [];
        end
        jremove(RaL);
        jremove(unrei);
        jremove(pair);
        jremove(taskids);
        jremove(ArrayList);
        jremove(Integer);

    else        
        error('Expected argument of type PAResL, received '+typeof(l))
    end
endfunction