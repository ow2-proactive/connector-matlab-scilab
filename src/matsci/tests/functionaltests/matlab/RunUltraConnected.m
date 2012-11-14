function ok=RunTestDisconnected(url, cred, tlbx_home, nbiter, testfunction, runAsMe)

fs = filesep();
addpath(tlbx_home);
addpath([tlbx_home fs 'Tests']);
oldpwd=pwd();
ok = true;
save('start.tst','ok');

PAoptions('Debug',true);
try
    PAconnect(url, cred);
    ok = true;
    disp('saving connect file');
    cd(oldpwd);
    save('connect.tst','ok');
    if runAsMe == 1
        PAoptions('RunAsMe',true);
    end
    eval([testfunction '(' num2str(nbiter) ');']);
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

exit();
end


