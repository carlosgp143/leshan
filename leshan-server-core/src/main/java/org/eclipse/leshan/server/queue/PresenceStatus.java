/*******************************************************************************
 * Copyright (c) 2017 RISE SICS AB.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v1.0 which accompany this distribution.
 * 
 * The Eclipse Public License is available at
 *    http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 *    http://www.eclipse.org/org/documents/edl-v10.html.
 * 
 * Contributors:
 *     RISE SICS AB - initial API and implementation
 *******************************************************************************/
package org.eclipse.leshan.server.queue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;

import org.eclipse.leshan.core.request.DownlinkRequest;
import org.eclipse.leshan.core.response.ErrorCallback;
import org.eclipse.leshan.core.response.LwM2mResponse;
import org.eclipse.leshan.core.response.ResponseCallback;

/**
 * Class that contains all the necessary elements to handle the queue mode. Every registration object that uses Queue
 * Mode has a PresenceStatus object linked to it for handling this mode.
 */

public class PresenceStatus {

    /* The state of the client: Awake or Sleeping */
    private Presence state;

    private ScheduledFuture<?> clientScheduledFuture;

    /* Queues to store requests */
    private  Map<DownlinkRequest<LwM2mResponse>, Long> syncRequestQueue;
    private List<AsyncStoredRequest> asyncRequestQueue;

    public PresenceStatus() {
        this.state = Presence.SLEEPING;
        this.syncRequestQueue = new HashMap<DownlinkRequest<LwM2mResponse>, Long>();
        this.asyncRequestQueue = new ArrayList<AsyncStoredRequest>();
    }

    /* Client State Control */

    /**
     * Set the client state to awake. This should be called when an update message is received from the server. It
     * starts the client awake timer.
     * 
     * @return true if the state was changed (previous state was {@link Presence#SLEEPING}
     */
    public boolean setAwake() {
        if (state == Presence.SLEEPING) {
            state = Presence.AWAKE;
            return true;
        }
        return false;
    }

    /**
     * Set the client state to sleeping. This should be called when the the time the client waits before going to sleep
     * expires, or when the client is not responding. It also notifies the listeners inside the {@link PresenceService}.
     * It stops the client awake timer.
     */

    /**
     * Set the client state to sleeping. This should be called when the the time the client waits before going to sleep
     * expires, or when the client is not responding. It also notifies the listeners inside the {@link PresenceService}.
     * It stops the client awake timer.
     * 
     * @return true if the state was changed (previous state was {@link Presence#AWAKE}
     */
    public boolean setSleeping() {
        if (state == Presence.AWAKE) {
            state = Presence.SLEEPING;
            return true;
        }
        return false;
    }

    /**
     * Tells if the client is awake or not
     * 
     * @return true if the status is {@link Presence#Awake}
     */
    public boolean isClientAwake() {
        return state == Presence.AWAKE;
    }

    /* Control of the time the client waits before going to sleep */

    /**
     * Sets the client scheduled task future, in order to cancel it.
     * 
     * @param clientScheduledFuture the scheduled future of the task.
     */
    public void setClientExecutorFuture(ScheduledFuture<?> clientScheduledFuture) {
        this.clientScheduledFuture = clientScheduledFuture;
    }

    /**
     * Gets the client scheduled task future, in order to cancel it.
     * 
     * @return the client scheduled task future.
     */
    public ScheduledFuture<?> getClientScheduledFuture() {
        return this.clientScheduledFuture;
    }

    public void addSyncRequestToQueue(DownlinkRequest<LwM2mResponse> request, long timeout) {
        this.syncRequestQueue.put(request, timeout);
    }

    public Map<DownlinkRequest<LwM2mResponse>, Long> getSyncRequestQueue() {
        return this.syncRequestQueue;
    }
    
    public void addAsyncRequestToQueue(DownlinkRequest<LwM2mResponse> request, long timeout,
            ResponseCallback<LwM2mResponse> responseCallback, ErrorCallback errorCallback) {
        this.asyncRequestQueue.add(new AsyncStoredRequest(request, timeout, responseCallback, errorCallback));
    }

    public List<AsyncStoredRequest> getAsyncRequestQueue() {
        return this.asyncRequestQueue;
    }
    
    public class AsyncStoredRequest {
    	DownlinkRequest<LwM2mResponse> request;
    	long timeout;
    	private ResponseCallback<LwM2mResponse> responseCallback;
    	private ErrorCallback errorCallback;
    	
    	public AsyncStoredRequest ( DownlinkRequest<LwM2mResponse> request, long timeout,
    			ResponseCallback<LwM2mResponse> responseCallback, ErrorCallback errorCallback) {
    		this.request = request;
    		this.timeout = timeout;
    		this.responseCallback = responseCallback;
    		this.errorCallback = errorCallback;
    	}
    	
    	public DownlinkRequest<LwM2mResponse> getRequest() {
    		return this.request;
    	}
    	
    	public long getTimeout() {
    		return this.timeout;
    	}
    	
    	public ResponseCallback<LwM2mResponse> getResponseCallback() {
    		return this.responseCallback;
    	}
    	
    	public ErrorCallback getErrorCallback() {
    		return this.errorCallback;
    	}
    	
    	
    }
    
    
}


