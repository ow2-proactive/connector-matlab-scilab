function b=%PAFiles_6(i1,f,a)

    if typeof(f) == 'PAFiles'
        b = mlist(['PAFiles','matrix']);
        b.matrix = f.matrix(1,i1);
    end

endfunction