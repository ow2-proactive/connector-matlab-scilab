function %PAResult_p(R)
    //disp('PAResult_p : '+typeof(l))
    if typeof(R) == 'PAResult' then        
        if ~jexists(R.waited)
            mprintf('Result Cleared\n');
            return;
        end
        if %t
            mprintf('Awaited (J:'+ string(R.jobid)+ ')\n')
        else
            //try
                disp(PAResult_PAwaitFor(R));
            //catch
                // error ignored in display
            //end
        end
    elseif typeof(R) == 'PAResL' then
        %PAResL_p(R);
    end

endfunction