function res=PAResult(taskinfo)
    global ('PAResult_DB');
    if typeof(PAResult_DB) ~= 'list'
        PAResult_DB = list();
    end    
    PAResult_DB($+1) = %f;
    dbrid = length(PAResult_DB);  
    cz_ab = jimport('java.util.concurrent.atomic.AtomicBoolean',%f);
    cz_ur = jimport('org.ow2.proactive.scheduler.ext.matsci.client.common.data.UnReifiable',%f);
    cz_sb = jimport('java.lang.StringBuilder', %f);
    res = tlist(['PAResult','cleanDir', 'outFile','jobid','taskid', 'cleaned', 'logsPrinted','logs','waited','iserror','resultSet','dbrid','sid','RaL'], taskinfo.cleanDir, taskinfo.outFile, taskinfo.jobid, taskinfo.taskid, jnewInstance(cz_ab,%f), jnewInstance(cz_ab,%f), jnewInstance(cz_sb), jnewInstance(cz_ab,%f), jnewInstance(cz_ab,%f),jnewInstance(cz_ab,%f), dbrid, taskinfo.sid, jnewInstance(cz_ur));
endfunction







