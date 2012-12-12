function res=PAFile(path)
     if argn(2) == 1
          res = mlist(['PAFile','Path','Space'],path,'automatic');
     else
          res = mlist(['PAFile','Path','Space'],[],'automatic');
     end

endfunction