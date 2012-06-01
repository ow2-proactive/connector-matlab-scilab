function [ok, msg]=TestCompose(nbiter,timeout)
    if ~exists('timeout')
        if (getos() == "Windows")
            timeout = 500000;
        else
            timeout = 200000;
        end
    end

    if ~exists('nbiter')
        nbiter = 1;
    end

    function [out]=mySqrt(in)    
        out=sqrt(in);
        //disp(out)
    endfunction

    function [ok,msg]=checkValuesSq(val,right)        
        if length(right) ~= length(val)
            ok = %f;
            msg = 'Wrong number of outputs';
        else
            for i=1:length(right)
                if typeof(val) == 'list'
                    if val(i) ~= right(i)
                        ok = %f;
                        msg = 'TestCompose::Wrong value of sqrt(sqrt(sqrt('+ string(i)+ '))), received '+ string(val(i))+ ', expected '+ string(right(i));
                    else
                        ok = %t;
                        msg = [];
                    end
                else
                    if val(i) ~= right(i)
                        ok = %f;
                        msg = 'TestCompose::Wrong value of sqrt(sqrt(sqrt('+ string(i)+ '))), received '+ string(val(i))+ ', expected '+ string(right(i));
                    else 
                        ok = %t;
                        msg = [];
                    end
                end
            end

        end
    endfunction

    format(20);
    t = PATask(3,5);
    t(1,1:5).Func = 'mySqrt';
    t(1,1).Params = list(1);
    t(1,2).Params = list(2);
    t(1,3).Params = list(3);
    t(1,4).Params = list(4);
    t(1,5).Params = list(5);
    t(2,1:5) = t(1,1:5);
    t(2,1:5).Params = list();
    t(2,1:5).Compose = %t;
    t(3,1:5) = t(2,1:5);

    for kk=1:nbiter
        disp('-------------------------------------');  
        disp('------------------------Iteration ' + string(kk));     
        disp('-------------------------------------');
        
        t(1,3).Params = list(3);
        
        disp('...... Testing PAsolve with sqrt(sqrt(sqrt(x)))');
        disp('..........................1 PAwaitFor');
        resl = PAsolve(t);
        val=PAwaitFor(resl,timeout)
        PAclearResults(resl);
        disp(val);
        [ok,msg]=checkValuesSq(val,sqrt(sqrt(sqrt(1:5))));
        if ~ok error(msg),return; end
        disp('..........................1 ......OK');
        clear val;

        disp('..........................2 PAwaitAny');
        resl = PAsolve(t);
        for i=1:5
            val(i)=PAwaitAny(resl,timeout)
        end

        disp(val);
        val=gsort(val,"g","i");
        [ok,msg]=checkValuesSq(val,sqrt(sqrt(sqrt(1:5))));
        PAclearResults(resl);
        if ~ok error(msg),return; end
        disp('..........................2 ......OK');
        clear val;

        disp('...... Testing PAsolve with sqrt(sqrt(sqrt(x))) and an error');
        t(1,3).Params = list('a');
        disp('..........................3 PAwaitFor (Error)');
        resl = PAsolve(t);
        val=PAwaitFor(resl(1:5),timeout);    
        [ok,msg]=checkValuesSq(val,list(sqrt(sqrt(sqrt(1))),sqrt(sqrt(sqrt(2))), [], sqrt(sqrt(sqrt(4))),sqrt(sqrt(sqrt(5)))));
        PAclearResults(resl);

        if ~ok error(msg),return; end
        disp('..........................3 ......OK');
        clear val;

        disp('..........................4 PAwaitAny (Error)');
        resl = PAsolve(t);

        msg = 'Error not received';    
        val=[];
        for i=1:5

            vl=PAwaitAny(resl,timeout)
            //disp(vl);
            if isempty(vl) then
                val(i) = 0; 
            else
                val(i) = vl;
            end                           
        end
        disp(val)    
        val=gsort(val,"g","i");
        [ok,msg]=checkValuesSq(val,list(0,sqrt(sqrt(sqrt(1))),sqrt(sqrt(sqrt(2))), sqrt(sqrt(sqrt(4))),sqrt(sqrt(sqrt(5)))));
        PAclearResults(resl);


        if ~ok error(msg),return; end
        disp('..........................4 ......OK');
        clear val;
    end

endfunction

