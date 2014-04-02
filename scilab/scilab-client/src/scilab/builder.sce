mode(-1);
lines(0);
global PA_matsci_dir

TOOLBOX_NAME = 'ProActiveConnector';
TOOLBOX_TITLE = 'ProActive Connector';

// Version Check
try
    version = getversion('scilab');
    if version(1) < 5
        error(gettext('Scilab 5.3.3 or more is required.'));
    elseif version(1) == 5 & version(2) < 3
        error(gettext('Scilab 5.3.3 or more is required.'));
    elseif version(1) == 5 & version(2) == 3 & version(3) < 3
        error(gettext('Scilab 5.3.3 or more is required.'));
    end
catch
    error(gettext('Scilab 5.3.3 or more is required.'));
end;

disp('Build ProActive');

root_tlbx = get_absolute_file_path('builder.sce');
if ~exists('PA_matsci_dir') | PA_matsci_dir == []
    if part(root_tlbx, length(root_tlbx)) == '/' then
       PA_matsci_dir=part(root_tlbx, 1:length(root_tlbx)-1);
    else
        PA_matsci_dir=root_tlbx;
    end
end
// ====================================================================
//if ~with_module('development_tools') then
//    error(msprintf(gettext('%s module not installed."),'development_tools'));
//end
macros=root_tlbx+'macros';


//ilib_build('importjava',sci_gtw,[cpp c jni gateway],[],'','-ljvm',include);

tbx_builder_macros(root_tlbx);
tbx_builder_help(root_tlbx);
tbx_build_loader(TOOLBOX_NAME, root_tlbx);
tbx_build_cleaner(TOOLBOX_NAME, root_tlbx);


//genlib('toolbox_pascheduler',macros);

clear root_tlbx TOOLBOX_NAME TOOLBOX_TITLE;

