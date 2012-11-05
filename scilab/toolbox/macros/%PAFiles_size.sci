function sz=%PAFiles_size(l,i)
    if argn(2) == 2 then
        sz=size(l.matrix,i);
    else
        sz=size(l.matrix,2);
    end
endfunction