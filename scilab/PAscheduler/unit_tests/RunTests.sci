function [ok, msg]=RunTests(nbiter,timeout)
    if ~exists('nbiter')
        nbiter = 1;
    end

    if exists('timeout')
        [ok,msg] = TestBasic(nbiter,timeout);
    else
        [ok,msg] = TestBasic(nbiter);
    end
    if ~ok disp(msg),return; end

    if exists('timeout')
        [ok,msg] = TestCompose(nbiter,timeout);
    else
        [ok,msg] = TestCompose(nbiter);
    end
    if ~ok disp(msg),return; end

    if exists('timeout')
        [ok,msg] = TestPATask(nbiter,timeout);
    else
        [ok,msg] = TestPATask(nbiter);
    end
    if ~ok disp(msg),return; end

    if exists('timeout')
        [ok,msg] = TestMultipleSubmit(nbiter,timeout);
    else
        [ok,msg] = TestMultipleSubmit(nbiter);
    end
    if ~ok disp(msg),return; end

    if exists('timeout')
        [ok,msg] = TestTopology(nbiter,timeout);
    else
        [ok,msg] = TestTopology(nbiter);
    end
    if ~ok disp(msg),return; end

    if exists('timeout')
        [ok,msg] = TestTransferEnv(nbiter,timeout);
    else
        [ok,msg] = TestTransferEnv(nbiter);
    end
    if ~ok disp(msg),return; end

    if exists('timeout')
        [ok,msg] = TestSelectionScripts(nbiter,timeout);
    else
        [ok,msg] = TestSelectionScripts(nbiter);
    end
    if ~ok disp(msg),return; end

endfunction