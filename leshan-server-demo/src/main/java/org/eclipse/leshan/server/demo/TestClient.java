package org.eclipse.leshan.server.demo;

import org.eclipse.leshan.Link;
import org.eclipse.leshan.core.node.codec.CodecException;
import org.eclipse.leshan.core.request.ContentFormat;
import org.eclipse.leshan.core.request.DownlinkRequest;
import org.eclipse.leshan.core.request.ExecuteRequest;
import org.eclipse.leshan.core.request.ReadRequest;
import org.eclipse.leshan.core.request.WriteRequest;
import org.eclipse.leshan.core.request.exception.ClientSleepingException;
import org.eclipse.leshan.core.request.exception.InvalidResponseException;
import org.eclipse.leshan.core.request.exception.RequestCanceledException;
import org.eclipse.leshan.core.request.exception.RequestRejectedException;
import org.eclipse.leshan.core.response.ExecuteResponse;
import org.eclipse.leshan.core.response.LwM2mResponse;
import org.eclipse.leshan.core.response.ReadResponse;
import org.eclipse.leshan.core.response.WriteResponse;
import org.eclipse.leshan.server.LwM2mServer;
import org.eclipse.leshan.server.registration.Registration;

public class TestClient {
	LwM2mServer server;
	public Link[] links;
	public Registration reg;
	public String endpoint;

	TestClient(Registration r, LwM2mServer server) {
		links = r.getSortedObjectLinks();
		reg = r;
		endpoint = r.getEndpoint();
		this.server = server;
	}

	public LwM2mResponse read(String path) {
		DownlinkRequest<ReadResponse> request = new ReadRequest(path);
		try {
			LwM2mResponse response = server.send(reg, request);
			return response;
		} catch (CodecException | InvalidResponseException | RequestCanceledException | RequestRejectedException
				| InterruptedException | ClientSleepingException e) {
			e.printStackTrace();
		}
		return null;
	}

	public LwM2mResponse readTLV(String path) {
		DownlinkRequest<ReadResponse> request = new ReadRequest(ContentFormat.TLV, path);
		try {
			LwM2mResponse response = server.send(reg, request);
			return response;
		} catch (CodecException | InvalidResponseException | RequestCanceledException | RequestRejectedException
				| InterruptedException | ClientSleepingException e) {
			e.printStackTrace();
		}
		return null;
	}

	public LwM2mResponse readJSON(String path) {
		DownlinkRequest<ReadResponse> request = new ReadRequest(ContentFormat.JSON, path);
		try {
			LwM2mResponse response = server.send(reg, request);
			return response;
		} catch (CodecException | InvalidResponseException | RequestCanceledException | RequestRejectedException
				| InterruptedException | ClientSleepingException e) {
			e.printStackTrace();
		}
		return null;
	}

	public LwM2mResponse execute(String path) {
		DownlinkRequest<ExecuteResponse> request = new ExecuteRequest(path);
		try {
			LwM2mResponse response = server.send(reg, request);
			return response;
		} catch (CodecException | InvalidResponseException | RequestCanceledException | RequestRejectedException
				| InterruptedException | ClientSleepingException e) {
			e.printStackTrace();
		}
		return null;
	}

	public LwM2mResponse write(int oid, int iid, int rid, String value) {
		DownlinkRequest<WriteResponse> request = new WriteRequest(oid, iid, rid, value);
		try {
			LwM2mResponse response = server.send(reg, request);
			return response;
		} catch (CodecException | InvalidResponseException | RequestCanceledException | RequestRejectedException
				| InterruptedException | ClientSleepingException e) {
			e.printStackTrace();
		}
		return null;
	}

	public LwM2mResponse write(int oid, int iid, int rid, long value) {
		DownlinkRequest<WriteResponse> request = new WriteRequest(oid, iid, rid, value);
		try {
			LwM2mResponse response = server.send(reg, request);
			return response;
		} catch (CodecException | InvalidResponseException | RequestCanceledException | RequestRejectedException
				| InterruptedException | ClientSleepingException e) {
			e.printStackTrace();
		}
		return null;
	}

}
