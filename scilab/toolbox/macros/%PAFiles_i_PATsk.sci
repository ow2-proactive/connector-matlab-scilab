function a=%PAFiles_i_PATsk(i1,b,a)
    if i1 == 'InputFiles' then
        a.InputFiles=b;
    elseif i1 == 'OutputFiles' then
        a.OutputFiles=b;
    else
        error('Type mismatch, '+i1+' doesn''t expects a PAFiles');
    end
endfunction