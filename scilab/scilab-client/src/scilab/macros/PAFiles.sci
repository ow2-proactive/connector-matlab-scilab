function res=PAFiles(c)
    res = mlist(['PAFiles','matrix']);
     if argn(2) == 0
        c=0;
     end
    res.matrix = cell(1,c);

    for n=1:c
       res.matrix{1,n} = PAFile();
    end

endfunction