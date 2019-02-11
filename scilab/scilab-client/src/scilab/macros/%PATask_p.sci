function %PATask_p(l)
    if typeof(l) == 'PATask' then
        n = size(l.matrix,1);
        m = size(l.matrix,2);
        for j=1:m
            for i=1:n
                patsk=l.matrix{i,j};
                mprintf('('+string(i)+','+string(j)+'):\n');
                %PATsk_p(patsk);
            end
        end
     end
endfunction