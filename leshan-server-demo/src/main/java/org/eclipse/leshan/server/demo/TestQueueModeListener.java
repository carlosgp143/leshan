/*******************************************************************************
 * Copyright (c) 2018 Sierra Wireless and others.
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
 *     Sierra Wireless - initial API and implementation
 *******************************************************************************/
package org.eclipse.leshan.server.demo;

import java.util.Date;

import org.eclipse.leshan.core.response.LwM2mResponse;
import org.eclipse.leshan.server.LwM2mServer;
import org.eclipse.leshan.server.queue.PresenceListener;
import org.eclipse.leshan.server.registration.Registration;
import org.python.util.PythonInterpreter;

public class TestQueueModeListener implements PresenceListener {

	private LwM2mServer server;
	private boolean awakeFirst;
	private boolean sleepFirst;

	public TestQueueModeListener(LwM2mServer server) {
		this.server = server;
		awakeFirst = true;
		sleepFirst = true;
	}

	@Override
	public void onAwake(final Registration registration) {
		if (awakeFirst) {
			System.out.println("Node is awake: " + registration.getEndpoint() + " at: " + new Date());
			System.out.println("Start tests...");
			TestClient testClient = new TestClient(registration, server);
			final LwM2mResponse respAwakeTime = testClient.read("30000/0/30000");
			final LwM2mResponse respSleepTime = testClient.read("30000/0/30001");

			new Thread(new Runnable() {
				@Override
				public void run() {

					try {
						PythonInterpreter interp = new PythonInterpreter();
						interp.exec("import sys");
						interp.set("respAwakeTime", respAwakeTime);
						interp.set("respSleepTime", respSleepTime);
						String fname = "pytests/test-qmode-awake.py";
						interp.execfile(fname);
						interp.close();
					} catch (Exception e) {
						e.printStackTrace();
					}
					System.out.println("Ending tests....");
				}
			}).start();
			awakeFirst = false;
		}
	}

	@Override
	public void onSleeping(final Registration registration) {
		if (sleepFirst) {
			System.out.println("Node is sleeping: " + registration.getEndpoint() + " at: " + new Date());
			new Thread(new Runnable() {
				@Override
				public void run() {
					System.out.println("Start tests...");
					try {

						PythonInterpreter interp = new PythonInterpreter();
						interp.exec("import sys");
						interp.set("client", new TestClient(registration, server));
						String fname = "pytests/test-qmode-sleep.py";
						interp.execfile(fname);
						interp.close();
					} catch (Exception e) {
						e.printStackTrace();
					}
					System.out.println("Ending tests....");
				}
			}).start();
			sleepFirst = false;
		}
	}

}
