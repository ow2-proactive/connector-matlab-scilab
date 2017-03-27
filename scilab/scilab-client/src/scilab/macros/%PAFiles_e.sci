function [b]=%PAFiles_e(i1,l)
    select typeof(i1)
    case 'constant' then
        if size(i1,2) == 1
           b = l.matrix{1,i1};
        else
            b = mlist(['PAFiles','matrix']);
            b.matrix = l.matrix(1,i1);
        end
    case 'string' then
        if size(l.matrix,2) == 1 then
            out = %PAFile_e(i1,l.matrix{1,1});
            b=out;
        else

            b=list();
            for j=1:size(l.matrix,2)
                out = %PAFile_e(i1,l.matrix{1,j});
                b($+1) = out;
            end
        end
        else
            error('unexpected index type: '+typeof(i1));
    end
endfunction