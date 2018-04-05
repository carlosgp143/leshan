/*******************************************************************************
 * Copyright (c) 2018 RISE SICS AB.
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

import org.eclipse.leshan.core.node.LwM2mSingleResource;
import org.eclipse.leshan.core.node.codec.CodecException;
import org.eclipse.leshan.core.request.DownlinkRequest;
import org.eclipse.leshan.core.request.ReadRequest;
import org.eclipse.leshan.core.request.exception.InvalidResponseException;
import org.eclipse.leshan.core.request.exception.RequestCanceledException;
import org.eclipse.leshan.core.request.exception.RequestRejectedException;
import org.eclipse.leshan.core.response.ReadResponse;
import org.eclipse.leshan.server.LwM2mServer;
import org.eclipse.leshan.server.registration.Registration;

public class QueueModeObjectClientAwakeTimeProvider implements ClientAwakeTimeProvider {

	private LwM2mServer server;
	private int clientAwakeTime;

	public QueueModeObjectClientAwakeTimeProvider(LwM2mServer server) {
		this.clientAwakeTime = 5000;
		this.server = server;
	}

	public QueueModeObjectClientAwakeTimeProvider(int defaultClientAwakeTime, LwM2mServer server) {
		this.clientAwakeTime = defaultClientAwakeTime;
		this.server = server;
	}

	@Override
	public int getClientAwakeTime(Registration reg) {
		DownlinkRequest<ReadResponse> request = new ReadRequest("6000/0/3000");
		try {
			ReadResponse response = server.send(reg, request);

			if (response != null && response.isSuccess()) {
				LwM2mSingleResource resource = (LwM2mSingleResource) response.getContent();
				switch (resource.getType()) {
				case INTEGER:
					this.clientAwakeTime = (int) resource.getValue();
					break;
				case STRING:
					this.clientAwakeTime = Integer.parseInt((String) resource.getValue());
				case BOOLEAN:
					break;
				case FLOAT:
					break;
				case OBJLNK:
					break;
				case OPAQUE:
					break;
				case TIME:
					break;
				default:
					break;
				}
			}

		} catch (CodecException | InvalidResponseException | RequestCanceledException | RequestRejectedException
				| InterruptedException e) {
			e.printStackTrace();
		}
		return this.clientAwakeTime;
	}

}
