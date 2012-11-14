function [ok, msg]=TestGetResults(nb_iter,index, timeout)
    if ~exists('timeout')
        if (getos() == "Windows")
            timeout = 500000;
        else
            timeout = 200000;
        end
    end

    function [out]=myWait(in)
        sleep(10000);
        out=%t;
        //disp(out)
    endfunction

    disp('---------------------------------------------');
    disp('...... Testing PAgetResults, iteration ' + string(index));

    if index > 1
        resl = PAgetResults(index-1);
        tf = PAisAwaited(resl);
        fd = find(tf == %t);
        if length(fd) > 0
            disp('Error ! the following results from iteration '+string(i)+' should be available :');
            disp(fd);
            disp('Exiting Scilab...')
            global('PA_jvminterface');
            jinvoke(PA_jvminterface,'shutdown');
            exit(1);
        end
    end
    disp('Submitting a new job...');
    resl = PAsolve('myWait',1,2,3,4);
    disp('Waiting for results');
    val=PAwaitFor(resl);
    disp(val);
    if (index < nb_iter)
        disp('Simulate crash!')
        global('PA_jvminterface');
        jinvoke(PA_jvminterface,'shutdown');
        exit();
    else
        disp('...... Test Successful');
        ok = %t;
        msg=[];
    end
endfunction