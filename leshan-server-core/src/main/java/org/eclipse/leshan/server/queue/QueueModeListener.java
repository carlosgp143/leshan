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
 *     Carlos Gonzalo Peces - initial API and implementation
 *******************************************************************************/

package org.eclipse.leshan.server.queue;

import org.eclipse.leshan.server.registration.Registration;

public interface QueueModeListener {
    
		/**
	     * This method is invoked when the LWM2M client with the given endpoint state changes from sleeping to awake. 
	     * 
	     * @param registration data of the lwm2m client.
	     */
	    void onAwake(Registration registration);
	
	    /**
	     * This method is invoked when the LWM2M client with the given endpoint state changes from awake to sleeping. 
	     * 
	     * @param registration data of the lwm2m client.
	     */
	    void onSleeping(Registration registration);
}
