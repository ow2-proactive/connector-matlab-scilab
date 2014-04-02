function c=%PAFiles_f_PAFiles(a,b)
    c = mlist(['PAFiles','matrix']);
    c.matrix = [a.matrix, b.matrix];
endfunction