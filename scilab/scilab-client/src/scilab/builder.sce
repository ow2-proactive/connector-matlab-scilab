mode(-1);
lines(0);
global PA_matsci_dir

// Version Check
version = getversion('scilab');
if version(1) < 6
	error(gettext('Scilab version higher than 6.0.0 is required.'));
end

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
tbx_build_loader(root_tlbx);
tbx_build_cleaner(root_tlbx);


//genlib('toolbox_pascheduler',macros);
