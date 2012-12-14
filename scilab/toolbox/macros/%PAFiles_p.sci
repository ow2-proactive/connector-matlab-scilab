function %PAFiles_p(l)
    if typeof(l) == 'PAFiles' then
        m = size(l.matrix,2);
         printf('{ ');
        //disp(m)
        for j=1:m
            patsk=l.matrix(1,j).entries;
            %PAFile_p(patsk);
            printf(' ');
        end
         printf('}\n');
     end
endfunction