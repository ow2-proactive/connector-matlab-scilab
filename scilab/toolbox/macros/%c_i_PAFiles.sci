function a=%c_i_PAFiles(i1,b,a)
    //disp(argn(2))
    if argn(2) == 3
        for i=1:size(a.matrix,1)
            for j=1:size(a.matrix,2)
                a.matrix(i,j).entries = %c_i_PAFile(i1,b,a.matrix(i,j).entries);
            end
        end
    else
        error('Not enough parameters');

    end
endfunction