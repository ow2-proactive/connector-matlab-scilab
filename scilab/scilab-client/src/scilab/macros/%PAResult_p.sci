function %PAResult_p(R)

	if typeof(R) == 'PAResult' then

		if ~jexists(R.waited)
            printf('Result Cleared\n');
        elseif PAisAwaited(PAResL(R)) then
			printf('Awaited (J:'+ string(R.jobid)+ ')\n')
        else
            disp(PAwaitFor(R));
        end

	elseif typeof(R) == 'PAResL' then
	    %PAResL_p(R);
	else
        error('Expected argument type received ' + typeof(R))
    end

endfunction