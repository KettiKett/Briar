package net.sf.briar.transport;

import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.briar.api.ContactId;
import net.sf.briar.api.db.DbException;
import net.sf.briar.api.protocol.TransportId;
import net.sf.briar.api.protocol.TransportIndex;
import net.sf.briar.api.protocol.batch.BatchConnectionFactory;
import net.sf.briar.api.protocol.stream.StreamConnectionFactory;
import net.sf.briar.api.transport.BatchTransportReader;
import net.sf.briar.api.transport.BatchTransportWriter;
import net.sf.briar.api.transport.ConnectionContext;
import net.sf.briar.api.transport.ConnectionDispatcher;
import net.sf.briar.api.transport.ConnectionRecogniser;
import net.sf.briar.api.transport.ConnectionRecogniser.Callback;
import net.sf.briar.api.transport.StreamTransportConnection;
import net.sf.briar.api.transport.TransportConstants;

import com.google.inject.Inject;

class ConnectionDispatcherImpl implements ConnectionDispatcher {

	private static final Logger LOG =
		Logger.getLogger(ConnectionDispatcherImpl.class.getName());

	private final ConnectionRecogniser recogniser;
	private final BatchConnectionFactory batchConnFactory;
	private final StreamConnectionFactory streamConnFactory;

	@Inject
	ConnectionDispatcherImpl(ConnectionRecogniser recogniser,
			BatchConnectionFactory batchConnFactory,
			StreamConnectionFactory streamConnFactory) {
		this.recogniser = recogniser;
		this.batchConnFactory = batchConnFactory;
		this.streamConnFactory = streamConnFactory;
	}

	public void dispatchReader(TransportId t, final BatchTransportReader r) {
		// Read the tag
		final byte[] tag;
		try {
			tag = readTag(r.getInputStream());
		} catch(IOException e) {
			if(LOG.isLoggable(Level.WARNING)) LOG.warning(e.getMessage());
			r.dispose(false);
			return;
		}
		// Get the connection context asynchronously
		recogniser.acceptConnection(t, tag, new Callback() {

			public void connectionAccepted(ConnectionContext ctx) {
				batchConnFactory.createIncomingConnection(ctx, r, tag);
			}

			public void connectionRejected() {
				r.dispose(true);
			}

			public void handleException(DbException e) {
				if(LOG.isLoggable(Level.WARNING)) LOG.warning(e.getMessage());
				r.dispose(false);
			}
		});
	}

	private byte[] readTag(InputStream in) throws IOException {
		byte[] b = new byte[TransportConstants.TAG_LENGTH];
		int offset = 0;
		while(offset < b.length) {
			int read = in.read(b, offset, b.length - offset);
			if(read == -1) throw new IOException();
			offset += read;
		}
		return b;
	}

	public void dispatchWriter(ContactId c, TransportIndex i,
			BatchTransportWriter w) {
		batchConnFactory.createOutgoingConnection(c, i, w);
	}

	public void dispatchIncomingConnection(TransportId t,
			final StreamTransportConnection s) {
		// Read the tag
		final byte[] tag;
		try {
			tag = readTag(s.getInputStream());
		} catch(IOException e) {
			if(LOG.isLoggable(Level.WARNING)) LOG.warning(e.getMessage());
			s.dispose(false);
			return;
		}
		// Get the connection context asynchronously
		recogniser.acceptConnection(t, tag, new Callback() {

			public void connectionAccepted(ConnectionContext ctx) {
				streamConnFactory.createIncomingConnection(ctx, s, tag);
			}

			public void connectionRejected() {
				s.dispose(true);
			}

			public void handleException(DbException e) {
				if(LOG.isLoggable(Level.WARNING)) LOG.warning(e.getMessage());
				s.dispose(false);
			}
		});
	}

	public void dispatchOutgoingConnection(ContactId c, TransportIndex i,
			StreamTransportConnection s) {
		streamConnFactory.createOutgoingConnection(c, i, s);
	}
}
