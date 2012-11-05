function a=%PAFiles_i_PAFiles(i1,b,a)
    if argn(2) == 3
        a.matrix(1,i1) = b.matrix;
    else
        error('unexpected number of arguments :'+argn(2));
    end
endfunction