function ok=transferenvfunc2(in)
    global totoGlobal
    mytransferenvvar = evalin('caller', 'toto');
    try
        disp(['Hello ' mytransferenvvar.field2]);

        ok = true;
    catch
        disp(getReport(ME));
        ok = false;
    end
    ok = true;
end