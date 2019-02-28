function %PAFiles_p(l)
    if typeof(l) == 'PAFiles' then
        m = size(l.matrix,2);
         mprintf('{ ');
        for j=1:m
            patsk=l.matrix{1,j};
            %PAFile_p(patsk);
            mprintf(' ');
        end
         mprintf('}\n');
     end
endfunction