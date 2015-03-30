function val_k=PAwaitFor(l,timeout)
    global('PA_solver')
    val_k = [];
    if typeof(l) == 'PAResL' then
        m = size(l.matrix,2);
        R=l.matrix(1).entries;
        jobid = R.jobid;
        jimport java.util.ArrayList;

        taskids = jnewInstance(ArrayList);
        allRes = %t;
        for i=1:m
            R = l.matrix(i).entries;
            // results already set
            rs = jinvoke(R.resultSet,'get');
            // results waited in PAwaitAny
            rw = jinvoke(R.waited,'get');            
            allRes = allRes & (rs | rw);
            jinvoke(taskids,'add',R.taskid); 
        end
        if ~allRes
            // if we haven't already received everything we trigger a waitAll call
            jimport java.lang.Integer;
            if argn(2) == 2                
                tout = jinvoke(Integer,'parseInt',string(timeout));
            else
                tout = jinvoke(Integer,'parseInt',string(-1));
            end
            try
                unrei = jinvoke(PA_solver,'waitAll',jobid,taskids, tout);
            catch
                // if an exception occur, it means the RMI stub is lost
                PAensureConnected();
                unrei = jinvoke(PA_solver,'waitAll',jobid,taskids, tout);
            end

            answers = jinvoke(unrei,'get');
            jremove(Integer);
        end

        val_k=list();
        anyerror = %f;
        for i=1:m
            R=l.matrix(i).entries;
            if ~allRes
                RaL = jinvoke(answers,'get',i-1);
                [tmpval,err] = PAResult_PAwaitFor(R,RaL);
                jremove(RaL);
                val_k($+1)=tmpval;
                if ~isempty(err) then
                    warning(err);
                    anyerror = %t;
                end
            else
                [tmpval,err] = PAResult_PAwaitFor(R);
                val_k($+1)=tmpval;
                if ~isempty(err) then
                    warning(err);
                    anyerror = %t;
                end
                
            end
        end
        if anyerror then
             warning('PAWaitFor:Error occurred');
        end
        if ~allRes
            jremove(answers);
            jremove(unrei);
        end
        jremove(taskids);
        jremove(ArrayList);

    elseif typeof(l) == 'PAResult' then
        R = l;
        // results already set
        rs = jinvoke(R.resultSet,'get');
        // results waited in PAwaitAny
        rw = jinvoke(R.waited,'get');
        allRes = (rs | rw);
        jobid = R.jobid;
        jimport java.util.ArrayList;
        anyerror = %f;
        taskids = jnewInstance(ArrayList);
        jinvoke(taskids,'add',R.taskid);
        if ~allRes
             // if we haven't already received everything we trigger a waitAll call
            jimport java.lang.Integer;
            if argn(2) == 2
                tout = jinvoke(Integer,'parseInt',string(timeout));
            else
                tout = jinvoke(Integer,'parseInt',string(-1));
            end
            try
                unrei = jinvoke(PA_solver,'waitAll',jobid,taskids, tout);
            catch
                // if an exception occurs, it means the RMI stub is lost
                PAensureConnected();
                unrei = jinvoke(PA_solver,'waitAll',jobid,taskids, tout);
            end

            answers = jinvoke(unrei,'get');
            jremove(Integer);

        end
        if ~allRes
            RaL = jinvoke(answers,'get',0);
            [tmpval,err] = PAResult_PAwaitFor(R,RaL);
            jremove(RaL);
            val_k=tmpval;
            if ~isempty(err) then
                warning(err);
                anyerror = %t;
            end
        else
            [tmpval,err] = PAResult_PAwaitFor(R);
            val_k=tmpval;
            if ~isempty(err) then
                warning(err);
                anyerror = %t;
            end
        end

        if anyerror then
             warning('PAWaitFor:Error occurred');
        end
        if ~allRes
            jremove(answers);
            jremove(unrei);
        end
        jremove(taskids);
        jremove(ArrayList);
    else        
        error('Expected argument of type PAResL, received '+typeof(l))
    end
endfunction
