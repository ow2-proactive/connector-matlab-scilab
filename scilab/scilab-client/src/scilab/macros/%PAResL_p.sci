function %PAResL_p(l)
    if typeof(l) == 'PAResL' then
        tf=PAisAwaited(l);
        m = size(l.matrix,2);
        fd = find(tf == %f);
        if length(fd) > 0 then
            PAwaitFor(l(fd)); 
        end               
        for i=1:m
            R=l.matrix{i};
            mprintf('('+string(i)+'):\n');
            if tf(i) then
                mprintf('Awaited (J:'+ string(R.jobid)+ ')\n');
            else
                disp(PAResult_PAwaitFor(R));
            end
                        
        end
    elseif typeof(l) == 'PAResult' then
        %PAResult_p(l);
    end

endfunction