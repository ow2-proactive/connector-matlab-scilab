function %PAResL_p(l)

    if typeof(l) == 'PAResL' then

		for i=1:size(l.matrix,2)
    		%PAResult_p( l.matrix(i).entries );
	    end
    elseif typeof(l) == 'PAResult' then

	    %PAResult_p(l);
    end

endfunction