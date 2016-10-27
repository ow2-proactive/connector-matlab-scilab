function [out]=myHello(in)
    nul = evalin('caller','NODE_URL_LIST'); 
    disp(['Number of nodes used : ' num2str(length(nul))]);
    for i=1:length(nul)
        disp(['Node n°' num2str(i) ': ' nul{i} ]);
    end
    disp(['Hello ' in]);
    out=true;
end