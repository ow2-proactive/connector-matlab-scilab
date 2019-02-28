function %PATsk_p(l)
    mprintf('Func: '+ pa_matrix2str(l.Func) +'\n');
    mprintf('Params:\n');
    for i=1:length(l.Params)
        disp(l.Params(i));
    end
    if ~isempty(l.Description) then
        mprintf('Description: '+ pa_matrix2str(l.Description) +'\n');
    end
    if ~isempty(l.InputFiles) then        
        mprintf('InputFiles: ');
        %PAFiles_p(l.InputFiles);
    end
    if ~isempty(l.OutputFiles) then
        mprintf('OutputFiles: ');
        %PAFiles_p(l.OutputFiles);
    end
    if ~isempty(l.SelectionScript) then
        mprintf('SelectionScript: '+l.SelectionScript+'\n');
    end
    if l.Static then
        mprintf('Static: true\n');
    else
        mprintf('Static: false\n');
    end
    if ~isempty(l.ScriptParams) then
        mprintf('ScriptParams: '+l.ScriptParams+'\n');
    end
    if (l.NbNodes > 1) then
        mprintf('NbNodes: '+string(l.NbNodes)+'\n');
    end
    if ~isempty(l.Topology) then
        mprintf('Topology: '+l.Topology+'\n');
    end
    if ~isempty(l.ThresholdProximity) then
        mprintf('ThresholdProximity: '+string(l.ThresholdProximity)+'\n');
    end
    if ~isempty(l.Sources) then
        mprintf('Sources: ');
        for i=1:length(l.Sources)
            mprintf('%s ', l.Sources(i));
        end 
        mprintf('\n');
    end
    if l.Compose then
        mprintf('Compose: true\n');
    else
        mprintf('Compose: false\n');
    end
    mprintf('\n');

endfunction
