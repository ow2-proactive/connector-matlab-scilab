function PAclearResults(l)
    if typeof(l) == 'PAResL' then
        m = size(l.matrix,2);
        R=l.matrix{1};
        //PAjobRemove(R.jobid);
        for i=1:m
            R=l.matrix{i};
            PAResult_clean(R);
            jremove(R.cleaned, R.logsPrinted, R.logs,R.waited, R.iserror, R.resultSet);
            try
                jremove(R.RaL)
            catch
            end            
        end
    elseif typeof(l) == 'PAResult' then
        PAResult_clean(l);
        jremove(l.cleaned, l.logsPrinted, l.logs,l.waited, l.iserror, l.resultSet);       
        try
            jremove(l.RaL)
        catch
        end     
    end
endfunction