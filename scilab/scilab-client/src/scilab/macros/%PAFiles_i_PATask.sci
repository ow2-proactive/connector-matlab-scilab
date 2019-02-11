function a=%PAFiles_i_PATask(i1,b,a)
    if argn(2) == 3
        for i=1:size(a.matrix,1)
            for j=1:size(a.matrix,2)
                a.matrix(i,j) = {%PAFiles_i_PATsk(i1,b,a.matrix{i,j})};
            end
        end
    else
        error('Not enough parameters');
    end
endfunction