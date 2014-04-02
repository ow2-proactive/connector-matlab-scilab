function a=%PAFile_i_PATsk(i1,b,a)
    if i1 == 'InputFiles' then
        c=PAFiles(1);
        c(1)=b;
        a.InputFiles=c;
    elseif i1 == 'OutputFiles' then
        c=PAFiles(1);
        c(1)=b;
        a.OutputFiles=c;
    else
        error('Type mismatch, '+i1+' doesn''t expects a PAFiles');
    end
endfunction