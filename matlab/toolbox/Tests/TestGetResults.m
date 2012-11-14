function [ok, msg]=TestGetResults(nb_iter,index, timeout)
    if ~exist('timeout', 'var')
        if ispc()
            timeout = 500000;
        else
            timeout = 200000;
        end
    end

    sched = PAScheduler;

    disp('---------------------------------------------');
    disp(['...... Testing PAgetResults, iteration ' num2str(index)]);

    if index > 1
        resl = PAgetResults(index-1);
        tf = PAisAwaited(resl);
        fd = find(tf == true);
        if length(fd) > 0
            disp(['Error ! the following results from iteration ' num2str(i) ' should be available :']);
            disp(fd);
            error('missing some previous results');
        end
    end
    disp('Submitting a new job...');
    resl = PAsolve(@myWait,1,2,3,4);
    disp('Waiting for results');
    val=PAwaitFor(resl);
    disp(val);
    if (index < nb_iter)
        disp('Simulate crash!')
        exit();
    else
        PAendSession();
        disp('...... Test Successful');
        ok = true;
        msg=[];

    end
end