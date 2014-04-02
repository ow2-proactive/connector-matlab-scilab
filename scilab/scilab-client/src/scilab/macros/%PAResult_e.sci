function b=%PAResult_e(i1, R)
    //disp('PAResult_e : '+typeof(l))
    //disp('PAResult_e : ');
    //disp(i1)
    select i1
    case 'val'
         b = PAwaitFor(R);
    case 'logs'
        if (jexists(R.logs)) then
            PAwaitFor(R);
            b = jinvoke(R.logs,'toString');
        else
            error('PAResult::object cleared');
        end
        
    case 'iserror'
        if (jexists(R.iserror)) then
            PAwaitFor(R);
            b = jinvoke(R.iserror, 'get');
        else
            error('PAResult::object cleared');
        end
    case 'jobid'
        b = R.jobid;
    case 'dbrid'
        b = R.dbrid;
    else
        error('PAResult::Unknown attribute : '''+i1+'''');    
    end

endfunction