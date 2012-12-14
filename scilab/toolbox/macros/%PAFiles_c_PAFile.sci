function c=%PAFiles_c_PAFile(a,f)
    c = mlist(['PAFiles','matrix']);
    cl = cell(1,1);
    cl(1,1).entries = f;
    if size(a.matrix,2) > 0
        c.matrix = cell(1,size(a.matrix,2)+1);
        c.matrix(1:size(a.matrix,2)) = a.matrix
        c.matrix(1,size(a.matrix,2)+1) = cl;
    else
        c.matrix = cl;
    end
endfunction