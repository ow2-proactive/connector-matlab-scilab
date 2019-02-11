function res = pa_matrix2str(mat)
    s = size(mat);
    if (s(1)==0)&(s(2)==0)
        res = '';
    elseif (s(1)==1)&(s(2)==1)
        res = string(mat);
    else
        res = "[";
        for i=1:s(1)
          if i > 1
            res = res + ";";
          end
          for j=1:s(2)
            if j > 1
              res = res + ",";
            end
            res = res + string(mat(i,j));
          end
        end
        res = res + "]";
    end
endfunction