function a=%c_i_PAFile(i1,b,a)
    if i1 == 'Path' then
        a.Path=b;
    elseif i1 == 'Space' then
        str = convstr(b,'l');
        if str == 'automatic' | str == 'global' | str == 'input' | str == 'output'
            a.Space = convstr(b,'l');
        else
            error('unknown value for Space : ' +str);
        end
    else
        error('Unknown attribute : '+i1);
    end
endfunction