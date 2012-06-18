lines(0);
oldpwd = pwd();
try
    //disp(oldpwd);
    if ~atomsIsInstalled('JIMS')
        disp('Warning JIMS is not installed, forcing JIMS installation');
        atomsInstall('JIMS');
    else
        atomsUpdate('JIMS');
    end

    exec builder.sce;
    cd unit_tests;
    exec buildtests.sce;
    cd ../
    ok = %t;
    save('ok.tst',ok);
catch
    disp(lasterror());
    sleep(1000);
    ok = %f;
    save('ko.tst',ok);
end
exit();

