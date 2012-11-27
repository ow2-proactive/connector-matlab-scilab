function pa_printf(form, exstr)
    exlen = length(exstr);
    ilen = exlen;
    llen = 0;
    //disp(ilen);
    while (ilen > 1000)
  //      mprintf('%d %d\n',llen+1,llen+1000)
        sstr = part(exstr,llen+1:llen+1000);
        llen = llen + 1000;
        ilen = ilen - 1000;
        mprintf(form,sstr);
    end
//    mprintf('%d %d\n',llen+1,ilen)
    sstr = part(exstr,llen+1:ilen);
    mprintf(form+'\n',sstr);
endfunction