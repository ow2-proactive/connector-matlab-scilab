function PAResult_clean(R)
    if ~jexists(R.cleaned) then
        return;
    end
    if ~jinvoke(R.cleaned,'get')
        warning('off')
       // setf = R.cleanFileSet;
        //for i=1:length(setf)
        //    mdelete(setf(i));
        //end
        dirToClean = R.cleanDir;
        //for i=1:length(setd)
            d=ls(dirToClean);
            if (getos() == "Linux") & size(d,1) == 2
                rmdir(dirToClean);
            elseif (getos() == "Windows") & size(d,1) == 0
                rmdir(dirToClean);
            end
        //end
        //sched = PAScheduler;
        //sched.PATaskRepository(R.jobid, R.taskid, 'received');
        jinvoke(R.cleaned,'set',%t);
        warning('on')
    end
endfunction