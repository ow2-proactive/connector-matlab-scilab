package org.ow2.proactive.scheduler.ext.matsci.middleman.proxy;

/**
 * A scheduler Event Listener. In addition to Scheduler events, supports data transfer related events.
 *
 * @author esalagea
 *
 */

import org.ow2.proactive.scheduler.common.SchedulerEventListener;


public interface ISchedulerEventListenerExtended extends SchedulerEventListener {

    //	public void pushDataFinished(String jobId, String pushLocation_URL);
    public void pullDataFinished(String jobId, String taskName, String localFolderPath);

    public void pullDataFailed(String jobId, String taskName, String remoteFolder_URL, Throwable t);
}