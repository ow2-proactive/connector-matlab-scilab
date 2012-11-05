function [tf] = PAendSession()
    PAensureConnected();
    repository org.ow2.proactive.scheduler.ext.matlab.client.embedded.MatlabTaskRepository.getInstance();
    listDirs = repository.dirsToCleanRec();
    sz_listDirs = listDirs.size();
    for i=0:sz_listDirs-1
        dirToClean = listDirs.get(i);
        rmdir(dirToClean,'s');
    end
    repository.endSession();
    solver = sched.PAgetsolver();
    pair = solver.endSession();
    tf = pair.getX();
    message = pair.getY();
    fprintf('%s\n',message);
end