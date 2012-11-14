function [tf] = PAbeginSession()
    PAensureConnected();
    repository = org.ow2.proactive.scheduler.ext.matlab.client.embedded.MatlabTaskRepository.getInstance();
    repository.beginSession();
    sched = PAScheduler();
    solver = sched.PAgetsolver();
    pair = solver.beginSession();
    tf = pair.getX();
    message = pair.getY();
    fprintf('%s\n',message);
end