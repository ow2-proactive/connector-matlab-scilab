function ok=RunUnitTest(url, cred, tlbx_home, nbiter, testfunction, runAsMe)

fs = filesep();
addpath(tlbx_home);
addpath([tlbx_home fs 'Tests']);
oldpwd=pwd();

try
    PAconnect(url, cred);
    PAoptions('Debug',true);
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
ok = true
exit();
end