function PAcleanDB()
    PAterminate();
    l1 = dir([tempdir 'SmartProxy.*']);
    l1 = {l1(:).name};
    l2 = dir([tempdir 'MatlabMiddlemanJobs.*']);
    l2 = {l2(:).name};
    l3 = dir([tempdir 'MatlabEmbeddedJobs.*']);
    l3 = {l3(:).name};
    l = [l1 l2 l3];
    for i = 1:length(l)
        delete([tempdir l{i}]);
    end
    
end