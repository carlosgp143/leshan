/*******************************************************************************
 * Copyright (c) 2017 Bosch Software Innovations GmbH and others.
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
 *     Bosch Software Innovations GmbH - initial API
 *     RISE SICS AB - added more features 
 *******************************************************************************/
package org.eclipse.leshan.server.queue;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.leshan.core.request.DownlinkRequest;
import org.eclipse.leshan.core.response.ErrorCallback;
import org.eclipse.leshan.core.response.LwM2mResponse;
import org.eclipse.leshan.core.response.ResponseCallback;
import org.eclipse.leshan.server.queue.PresenceStatus.AsyncStoredRequest;
import org.eclipse.leshan.server.registration.Registration;

/**
 * Tracks the status of each LWM2M client registered with Queue mode binding. Also ensures that the
 * {@link PresenceListener} are notified on state changes only for those LWM2M clients registered using Queue mode
 * binding.
 * 
 * @see Presence
 */
public final class PresenceServiceImpl implements PresenceService {

    private final ConcurrentMap<String, PresenceStatus> clientStatusList = new ConcurrentHashMap<>();
    private final List<PresenceListener> listeners = new CopyOnWriteArrayList<>();
    private final ClientAwakeTimeProvider awakeTimeProvider;
    private final ScheduledExecutorService clientTimersExecutor = Executors.newSingleThreadScheduledExecutor();
    private QueueModeLwM2mRequestSender requestSender;

    public PresenceServiceImpl(ClientAwakeTimeProvider awakeTimeProvider) {
        this.awakeTimeProvider = awakeTimeProvider;
    }

    @Override
    public void addListener(PresenceListener listener) {
        listeners.add(listener);
    }

    @Override
    public void removeListener(PresenceListener listener) {
        listeners.remove(listener);
    }

    @Override
    public boolean isClientAwake(Registration registration) {
        PresenceStatus presenceStatus = clientStatusList.get(registration.getEndpoint());
        if (presenceStatus == null) {
            return false;
        }
        return presenceStatus.isClientAwake();

    }

    /**
     * Set the state of the client identified by registration as {@link Presence#AWAKE}
     * 
     * @param reg the client's registration object
     */
    public void setAwake(Registration reg) {
        if (reg.usesQueueMode()) {
            PresenceStatus status = new PresenceStatus();
            PresenceStatus previous = clientStatusList.putIfAbsent(reg.getEndpoint(), status);
            if (previous != null) {
                // We already have a status for this reg.
                status = previous;
            }

            boolean stateChanged = false;
            synchronized (status) {

                // Every time we set the clientAwakeTime, in case it changes dynamically
                stateChanged = status.setAwake();
                startClientAwakeTimer(reg, status, awakeTimeProvider.getClientAwakeTime(reg));
                if (stateChanged) {
                    sendSyncQueuedRequests(reg);
                    sendAsyncQueuedRequests(reg);
                }
            }

            if (stateChanged) {
                for (PresenceListener listener : listeners) {
                    listener.onAwake(reg);
                }
            }
        }
    }

    /**
     * Notify the listeners that the client state changed to {@link Presence#SLEEPING}. The state changes is produced
     * inside {@link PresenceStatus} when the timer expires or when the client doesn't respond to a request.
     * 
     * @param reg the client's registration object
     */
    public void setSleeping(Registration reg) {
        if (reg.usesQueueMode()) {
            PresenceStatus status = clientStatusList.get(reg.getEndpoint());

            if (status != null) {
                boolean stateChanged = false;
                synchronized (status) {
                    stateChanged = status.setSleeping();
                    stopClientAwakeTimer(reg);
                }
                if (stateChanged) {
                    for (PresenceListener listener : listeners) {
                        listener.onSleeping(reg);
                    }
                }
            }
        }
    }

    /**
     * Removes the {@link PresenceStatus} object associated with the client from the list.
     * 
     * @param reg the client's registration object.
     */
    public void removePresenceStatusObject(Registration reg) {
        clientStatusList.remove(reg.getEndpoint());
    }

    /**
     * Returns the {@link PresenceStatus} object associated with the given endpoint name.
     * 
     * @param reg The client's registration object.
     * @return The {@link PresenceStatus} object.
     */
    private PresenceStatus getPresenceStatusObject(Registration reg) {
        return clientStatusList.get(reg.getEndpoint());
    }

    /**
     * Start or restart (if already started) the timer that handles the client wait before sleep time.
     * 
     * @param status
     */
    public void startClientAwakeTimer(final Registration reg, PresenceStatus clientPresenceStatus,
            int clientAwakeTime) {

        ScheduledFuture<?> clientScheduledFuture = clientPresenceStatus.getClientScheduledFuture();

        if (clientAwakeTime != 0) {
            if (clientScheduledFuture != null) {
                clientScheduledFuture.cancel(true);
            }
            clientScheduledFuture = clientTimersExecutor.schedule(new Runnable() {

                @Override
                public void run() {
                    if (isClientAwake(reg)) {
                        setSleeping(reg);
                    }
                }
            }, clientAwakeTime, TimeUnit.MILLISECONDS);
            clientPresenceStatus.setClientExecutorFuture(clientScheduledFuture);
        }

    }

    /**
     * Stop the timer that handles the client wait before sleep time.
     */
    private void stopClientAwakeTimer(Registration reg) {
        PresenceStatus clientPresenceStatus = getPresenceStatusObject(reg);
        ScheduledFuture<?> clientScheduledFuture = clientPresenceStatus.getClientScheduledFuture();
        if (clientScheduledFuture != null) {
            clientScheduledFuture.cancel(true);
        }
    }

    /**
     * Called when the client doesn't respond to a request, for changing its state to SLEEPING
     */
    public void clientNotResponding(Registration reg) {
        if (isClientAwake(reg)) {
            setSleeping(reg);
        }
    }

    public void addSyncRequestToQueue(DownlinkRequest<LwM2mResponse> request, long timeout, Registration reg,
            QueueModeLwM2mRequestSender requestSender) {
        System.out.println("Add request called");
        if (this.requestSender == null) {
            this.requestSender = requestSender;
        }
        PresenceStatus presenceStatus = clientStatusList.get(reg.getEndpoint());
        if (presenceStatus != null) {
            presenceStatus.addSyncRequestToQueue(request, timeout);
            System.out.println("Add request done");
        }
    }
    
    public void addAsyncRequestToQueue(DownlinkRequest<LwM2mResponse> request, long timeout, 
            ResponseCallback<LwM2mResponse> responseCallback,
    		ErrorCallback errorCallback, Registration reg, QueueModeLwM2mRequestSender requestSender) {
        System.out.println("Add request called");
        if (this.requestSender == null) {
            this.requestSender = requestSender;
        }
        PresenceStatus presenceStatus = clientStatusList.get(reg.getEndpoint());
        if (presenceStatus != null) {
            presenceStatus.addAsyncRequestToQueue(request, timeout, responseCallback, errorCallback);
            System.out.println("Add request done");
        }
    }

    private void sendSyncQueuedRequests(Registration reg) {
        System.out.println("Send Sync Queue Request called");
        if (requestSender == null) {
            System.out.println("No request sender");
        }
        if (this.requestSender != null) {
            System.out.println("requestSender not null");
            PresenceStatus presenceStatus = clientStatusList.get(reg.getEndpoint());
            if (presenceStatus != null) {
                System.out.println("Queue Length: " + presenceStatus.getSyncRequestQueue().size());
                for (Map.Entry<DownlinkRequest<LwM2mResponse>, Long> pair : 
                    presenceStatus.getSyncRequestQueue().entrySet()) {
                    System.out.println("Iteration");
                    try {
                        requestSender.send(reg, pair.getKey(), pair.getValue());
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            }
            presenceStatus.getSyncRequestQueue().clear();
        }

    }
    
    private void sendAsyncQueuedRequests(Registration reg) {
        System.out.println("Send Async Queue Request called");
        if (requestSender == null) {
            System.out.println("No request sender");
        }
        if (this.requestSender != null) {
            System.out.println("requestSender not null");
            PresenceStatus presenceStatus = clientStatusList.get(reg.getEndpoint());
            if (presenceStatus != null) {
                System.out.println("Queue Length: " + presenceStatus.getAsyncRequestQueue().size());
                for (AsyncStoredRequest asyncRequest : presenceStatus.getAsyncRequestQueue()) {
                    System.out.println("Iteration");
                    requestSender.send(reg, asyncRequest.getRequest(), asyncRequest.getTimeout(),
							asyncRequest.getResponseCallback(), asyncRequest.getErrorCallback());
                }
            }
            presenceStatus.getAsyncRequestQueue().clear();
        }

    }

    public void setRequestSender(QueueModeLwM2mRequestSender requestSender) {
        this.requestSender = requestSender;
    }
}
