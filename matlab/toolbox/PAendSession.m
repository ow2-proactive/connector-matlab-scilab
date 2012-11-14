function [tf] = PAendSession()
    PAensureConnected();
    repository = org.ow2.proactive.scheduler.ext.matlab.client.embedded.MatlabTaskRepository.getInstance();
    repository.endSession();
    sched = PAScheduler();
    solver = sched.PAgetsolver();
    pair = solver.endSession();
    tf = pair.getX();
    message = pair.getY();
    fprintf('%s\n',message);
end