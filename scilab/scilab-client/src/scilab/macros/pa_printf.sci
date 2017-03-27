// Moving to mprintf('%s',xx) to mprintf(xx) due to
// Scilab ISSUE #14915: ascii(10) makes mprintf() stop printing

function pa_printf(exstr)
    exlen = length(exstr);
    ilen = exlen;
    llen = 0;
    while (ilen > 1000)
        sstr = part(exstr,llen+1:llen+1000);
        llen = llen + 1000;
        ilen = ilen - 1000;
        mprintf(sstr);
    end
    sstr = part(exstr,llen+1:ilen);
    mprintf(sstr);
endfunction