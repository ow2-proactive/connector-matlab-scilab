function PAterminate()
opt = PAoptions();
try
    sched = PAScheduler;
    
    solver = sched.PAgetsolver();
    if strcmp(class(solver),'double')
        return;
    end
    
    if ischar(opt.SharedPushPublicUrl) & ~isempty(opt.SharedPushPublicUrl)
        % ok data is handled in a shared data space
    else
        if opt.EnableDisconnectedPopup
            trepository = org.ow2.proactive.scheduler.ext.matlab.client.embedded.MatlabTaskRepository.getInstance();
            notReceived = trepository.notYetReceived();
            if ~notReceived.isEmpty()
                msg = ['The following jobs are not completed yet : '];
                for j = 0:notReceived.size()-1
                    msg = [msg char(notReceived.get(j)) ' '];
                end
                msg = [msg 10];
                msg = [msg 'The MiddleMan JVM can stay alive and handle data transfers while your computer is on' 10];
                msg = [msg 'Do you want enable this mode ?'];
                button = questdlg(msg,'Disconnect','Yes','No','Yes');
                if strcmp(button, 'Yes')
                    return;
                end
            end
        end
    end
    
    jvm = sched.PAgetJVMInterface();
    jvm.shutdown();
    
catch ME
    disp('There was a problem during the finish script. Displaying the error during 10 seconds...');
    if isa(ME,'MException')
        disp(getReport(ME));
    elseif isa(ME, 'java.lang.Throwable')
        ME.printStackTrace();
    end
    pause(10);
end
end