function [ok, msg]=TestTransferEnv(nbiter,timeout)
    if ~exists('timeout')
        if (getos() == "Windows")
            timeout = 500000;
        else
            timeout = 200000;
        end
    end
    if ~exists('nbiter')
        nbiter = 1;
    end

    function [out]=myHello(in)
         global('totoGlobal')
         disp(toto);
         disp(totoGlobal);
         out=%t;
    endfunction

    function [out]=myHello2(in)
         disp(toto);
         out=%t;
    endfunction


    opt=PAoptions();
    oldTransferEnv = opt.TransferEnv;
    PAoptions('TransferEnv',%t);
    for kk=1:nbiter
        disp('-------------------------------------');  
        disp('------------------------Iteration ' + string(kk));     
        disp('-------------------------------------');
        disp('...... Testing PAsolve with TransferEnv');
        disp('..........................1 Local');
        toto = 'Hello Toto';

        r=PAsolve('myHello2',%t);
        val=PAwaitFor(r,timeout);
        ok = val(1);
        PAclearResults(r);
        disp('..........................1 Local+Global');
        toto = 'Hello Toto';
        global ('totoGlobal');
        totoGlobal = 'Hello Toto Global';



        r=PAsolve('myHello',%t);
        val=PAwaitFor(r,timeout);
        ok = val(1);
        PAclearResults(r);

        if ok then
            disp('................................OK');
        end
        msg=[];
    end
    PAoptions('TransferEnv',oldTransferEnv);
endfunction

