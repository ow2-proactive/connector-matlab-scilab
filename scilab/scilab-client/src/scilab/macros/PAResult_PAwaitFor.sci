function [val_k,err]=PAResult_PAwaitFor(R,RaL)
    global ('PAResult_DB');
    val_k = [];
    //disp('PAResult_PAwaitFor : init')    
    errormessage=[];
    //if ~jexists(R.future) then
    //    error('PAResult::object cleared');        
    //end
       
    if argn(2) == 2        
        jinvoke(R.RaL,'set',RaL);        
    else
        RaL = jinvoke(R.RaL,'get');                
    end
    if jinvoke(RaL,'isOK')
        //disp('PAResult_PAwaitFor : isOK')        
        printLogs(R,RaL, %f);

        if jinvoke(R.resultSet,'get')
            //disp('PAResult_PAwaitFor : Result get')
            val_k=PAResult_DB(R.dbrid);
            //disp(val)
        else
            //disp('PAResult_PAwaitFor : Result set')
            load(R.outFile);
            val_k = out;
            PAResult_DB(R.dbrid) = out;
            //disp(val)
            resultSet(R);
        end

    elseif jinvoke(RaL,'isMatSciError');
        //disp('PAResult_PAwaitFor : isMatSciError')
        
        printLogs(R,RaL,%t);
        jinvoke(R.iserror,'set',%t);
        resultSet(R);
        errormessage = 'PAResult:PAwaitFor Error during execution of task '+R.taskid;
    else
        //disp('PAResult_PAwaitFor : internalError')
        printLogs(R,RaL,%t);
        e = jinvoke(RaL,'getException');
        jimport org.ow2.proactive.scheduler.ext.common.util.StackTraceUtil;        
        exstr = jinvoke(StackTraceUtil,'getStackTrace',e);
        pa_printf(exstr);
        
        try
            jremove(e);            
            //jremove(ScilabSolver);
        catch 
        end        
        jinvoke(R.iserror,'set',%t);
        resultSet(R);
        jremove(StackTraceUtil);
        errormessage = 'PAResult:PAwaitFor Internal Error while executing '+R.taskid;
    end    
    jremove(RaL);
    PAResult_clean(R);
    err = errormessage;
endfunction

function resultSet(R)
    if ~jinvoke(R.resultSet,'get') then
        jinvoke(R.resultSet,'set',%t);

        jimport org.ow2.proactive.scheduler.ext.scilab.client.embedded.ScilabTaskRepository;
        repository = jinvoke(ScilabTaskRepository,'getInstance');
        jinvoke(repository,'receivedTask',R.jobid, R.taskid);
        tf = jinvoke(repository,'allReceived',R.jobid)
        jremove(repository);
        jremove(ScilabTaskRepository);
        opt = PAoptions();
        if opt.RemoveJobAfterRetrieve & tf then
            PAjobRemove(R.jobid);
        end
    end
endfunction

function printLogs(R,RaL,err)
    if ~jexists(R.logsPrinted) then
            error('PAResult::object cleared');
        end
    if ~jinvoke(R.logsPrinted,'get')
        logs = jinvoke(RaL,'getLogs');
        dummy = jinvoke(R.logs,'append',logs);
        //jremove(logs);
        jremove(dummy); // append returns a StringBuilder object that must be freed
        logstr = jinvoke(R.logs,'toString');
        if ~isempty(logstr) then
            pa_printf(logstr + '\n');
        end
        jinvoke(R.logsPrinted,'set',%t);
    elseif err
        logstr = jinvoke(R.logs,'toString');
        if ~isempty(logstr) then
            pa_printf(logstr + '\n');
        end
    end    
endfunction

