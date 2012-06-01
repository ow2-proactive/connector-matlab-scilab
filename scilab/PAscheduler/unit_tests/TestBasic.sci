function [ok, msg]=TestBasic(nbiter,timeout)
    if ~exists('timeout')
        if (getos() == "Windows")
            timeout = 200000;
        else
            timeout = 80000;
        end
    end
    
    if ~exists('nbiter')
        nbiter = 1;
    end

    function [out]=funcSquare(in)
        printf('in='+string(in)+'\n');
        if typeof(in) == 'string'
            error('char argument not expected');
        end
        out=in*in;     
    endfunction


    function [ok,msg]=checkValuesFact(val)
        [ok,msg]=checkValues(val,list(1,2,6,24,120),'factorial');
    endfunction

    function [ok,msg]=checkValuesSquare(val)
        [ok,msg]=checkValues(val,list(1,4,9,[],25), 'funcSquare');
    endfunction

    function [ok,msg]=checkValuesSquare2(val)
        [ok,msg]=checkValues(val,list([],1,4,9,25), 'funcSquare');
    endfunction
    
    function [ok,msg]=checkValuesSquare4(val)
        [ok,msg]=checkValues(val,list(0,1,4,9,25), 'funcSquare');
    endfunction

    function [ok,msg]=checkValuesSquare3(val)
        [ok,msg]=checkValues(val,list(1,4,9), 'funcSquare');
    endfunction

    function [ok,msg]=checkValues(val,right,name)
    if length(right) ~= length(val)
        ok = %f;
        msg = 'Wrong number of outputs';
    else
        for i=1:length(right)
            if typeof(val) == 'list'
                if val(i) ~= right(i)
                    ok = %f;
                    msg = 'TestBasic::Wrong value of '+ name+ '(' + string(i) +'), received '+ string(val(i))+ ', expected ' + string(right(i));                
                else
                    ok = %t;
                    msg = [];
                end
            else
                if val(i) ~= right(i)
                    ok = %f;
                    msg = 'TestBasic::Wrong value of '+ name +'(' + string(i)+ '), received '+ string(val(i))+ ', expected ' + string(right(i));

                else 
                    ok = %t;
                    msg = [];
                end
            end
        end
        if ~ok then
            disp('Received:');
            disp(val)
        end

    end
    endfunction
for kk=1:nbiter 
    disp('-------------------------------------');  
    disp('------------------------Iteration ' + string(kk));     
    disp('-------------------------------------');
    disp('...... Testing PAsolve with factorial');
    disp('..........................1 PAwaitFor');
    resl = PAsolve('factorial',1,2,3,4,5);
    val=PAwaitFor(resl,timeout)
    disp(val);    
    [ok,msg]=checkValuesFact(val);
    PAclearResults(resl);
    if ~ok error(msg); end
    disp('..........................1 ......OK');
    clear val;

    disp('..........................2 PAwaitAny');
    resl = PAsolve('factorial',1,2,3,4,5);
    for i=1:5
        val(i)=PAwaitAny(resl,timeout)
    end    
    disp(val);
    val=gsort(val,"g","i");
    [ok,msg]=checkValuesFact(val);
    PAclearResults(resl);
    if ~ok error(msg); end
    disp('..........................2 ......OK');
    clear val;


    disp('...... Testing PAsolve with funcSquare and an error');
    disp('..........................3 PAwaitFor');
    resl = PAsolve('funcSquare',1,2,3,'a',5);
    val=PAwaitFor(resl(1:5),timeout)            
    disp(val)
    [ok,msg]=checkValuesSquare(val);
    PAclearResults(resl);
    if ~ok error(msg); end
    disp('..........................3 ......OK');
    clear val;

    disp('..........................4 PAwaitAny');
    resl = PAsolve('funcSquare',1,2,3,'a',5);
    val=[];
    for i=1:5
        //printf('k=%d\n',k)        
        vl=PAwaitAny(resl,timeout);        
        //disp(vl)
        if isempty(vl) then
            val(i) = 0; 
        else
            val(i) = vl;
        end                              
    end
    disp(val)
    val=gsort(val,"g","i");

    [ok,msg]=checkValuesSquare4(val);
    PAclearResults(resl);
    if ~ok error(msg); end
    disp('..........................4 ......OK');
    clear val;
end

endfunction

