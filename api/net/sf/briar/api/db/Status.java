package net.sf.briar.api.db;

/** The status of a message with respect to a particular contact. */
public enum Status {
	/** The message has not been sent, received, or acked. */
	NEW,
	/** The message has been sent, but not received or acked. */
	SENT,
	/** The message has been received or acked. */
	SEEN
}
