function a=%c_i_PAFile(i1,b,a)
    if i1 == 'Path' then
        a.Path=b;
    elseif i1 == 'Space' then
        a.Space = convstr(b,'l');
    else
        error('Type mismatch, '+i1+' doesn''t expects a string');
    end
endfunction