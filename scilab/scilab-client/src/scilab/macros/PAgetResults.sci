function outputs = PAgetResults(jobid)
global ('PA_connected');
if or(type(jobid)==[1 5 8]) then
    jobid = string(jobid);
end
if ~exists('PA_connected') | PA_connected ~= 1
      error('PAensureConnected::Connection to the scheduler must be first established, see PAconnect.');
      return;
end
jimport org.ow2.proactive.scheduler.ext.scilab.client.embedded.ScilabTaskRepository;
trep = jinvoke(ScilabTaskRepository,'getInstance');
jobinfo = jinvoke(trep, 'getInfo', jobid);

jremove(trep);
jremove(ScilabTaskRepository);
disp('Retrieving results of job '+jobid);

ftnjava = jinvoke(jobinfo,'getFinalTasksNamesAsList');
dir_to_clean_java = jinvoke(jobinfo,'getDirToClean');
dir_to_clean = string(dir_to_clean_java);
jremove(dir_to_clean_java);
NN = jinvoke(ftnjava,'size');
ftn = list();

taskinfo = struct('cleanDir',[], 'outFile',[], 'jobid',[], 'taskid',[] );
results=list(NN);
for i=1:NN
    tidjava = jinvoke(ftnjava,'get',i-1);

            ftn(i) = string(tidjava);
            jremove(tidjava);
            taskinfo.cleanDir = dir_to_clean;
            out_path_java = jinvoke(jobinfo,'getOutputVariablePathWithIndex',i-1);

            taskinfo.outFile = string(out_path_java);
            taskinfo.jobid = jobid;

            taskinfo.taskid = ftn(i);

            results(i)=PAResult(taskinfo);
end
outputs = PAResL(results);
endfunction