function ok=RunTestDisconnected(url, cred, tlbx_home, nbiter,index, testfunction, runAsMe)

fs = filesep();
addpath(tlbx_home);
addpath([tlbx_home fs 'Tests']);
oldpwd=pwd();
ok = true;
save('start.tst','ok');

PAoptions('Debug',true);
try
    PAconnect(url, cred);
    if runAsMe == 1
        PAoptions('RunAsMe',true);
    end
    eval([testfunction '(' num2str(nbiter) ',' num2str(index) ');']);
    ok = true;
    disp('saving ok file');
    cd(oldpwd);
    save('ok.tst','ok');
catch ME
    disp(getReport(ME));

    pause(1);
    ok = false;
    cd(oldpwd);
    save('ko.tst','ok');
end
sched = PAScheduler;
itf = sched.PAgetJVMInterface();
itf.shutdown();
pause(1);
ok = true
exit();
end


