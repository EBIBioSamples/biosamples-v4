package uk.ac.ebi.biosamples.messages.threaded;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicMarkableReference;
import java.util.concurrent.atomic.AtomicReference;

public class MessageSampleStatus<S> {

	public final AtomicBoolean storedInRepository = new AtomicBoolean(false);
	public final AtomicReference<Exception> hadProblem = new AtomicReference<>(null);
	public final long messageTime;
	
	public final S sample;
	
	private MessageSampleStatus(S sample, long messageTime) {
		this.sample = sample;
		this.messageTime = messageTime;
	}
	
	public static <S> MessageSampleStatus<S> build(S sample, long messageTime) {
		if (sample == null) throw new IllegalArgumentException("sample cannot be null");
		return new MessageSampleStatus<>(sample, messageTime);
	}
}
