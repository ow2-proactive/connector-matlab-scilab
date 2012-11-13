lines(0);
oldpwd = pwd();
//disp(oldpwd);

if ~atomsIsLoaded('JIMS')
    // due to a bug that JIMS is sometimes not loaded at Scilab start
    ok = %t;
    save('re.tst',ok);
    exit();
end

exec loader.sce;
cd unit_tests;
exec buildtests.sce;
args = sciargs();
url = args(6);
cred = args(7);
nbiter = string(args(8));
index = string(args(9));
testfunction = args(10);
runAsMe = args(11);

PAoptions('Debug',%t);
try
    PAconnect(url, cred);
    if evstr(runAsMe) == 1
        PAoptions('RunAsMe',%t);
    end
    evstr(testfunction+'('+nbiter+','+index+');');
    ok = %t;
    disp('saving ok file');
    cd(oldpwd);
    warning('off');
    save('ok.tst',ok);
    warning('on');
catch
    disp(lasterror());

    sleep(1000);
    ok = %f;
    cd(oldpwd);
    warning('off');
    save('ko.tst',ok);
    warning('on');
end
global('PA_jvminterface');
jinvoke(PA_jvminterface,'shutdown');
sleep(1000);
exit();