function PAterminate()
    global ('PA_jvminterface')
    disp('quitting...');
    opt = PAoptions();

    if type(PA_jvminterface) ~= 1 then

        if type(opt.SharedPushPublicUrl) ~= 1
            // ok data is handled in a shared data space
        else
            if opt.EnableDisconnectedPopup
                jimport org.ow2.proactive.scheduler.ext.scilab.client.embedded.ScilabTaskRepository;
                repository = jinvoke(ScilabTaskRepository,'getInstance');
                notReceived = jinvoke(repository, 'notYetReceived');
                jobs = [];

                // Retrieve the number of uncomplete jobs before last scilab shutdown
                if type(notReceived) == 17
                    notReceivedSize = jinvoke(notReceived, 'size');
                else // type == 10
                    notReceivedSize = size(notReceived,'c');
                end

                if notReceivedSize ~= 0
                    msg = 'The following jobs are not completed yet : ';
                    for j = 1:notReceivedSize
                        jid = notReceived(j);
                        msg = msg + ' ' + jid;
                    end
                    msg = msg + ascii(10);
                    msg = msg + 'The MiddleMan JVM can stay alive and handle data transfers while your computer is on' + ascii(10);
                    msg = msg + 'Do you want enable this mode ?';
                    btn = messagebox(msg, "ProActive", "question", ["Yes" "No"], "modal")
                    if btn == 1
                        return;
                    end
                end
                jremove(notReceived);
                jremove(repository);
                jremove(ScilabTaskRepository);
            end
        end
        try
            jinvoke(PA_jvminterface,'shutdown');
        catch
        end
    end
endfunction
