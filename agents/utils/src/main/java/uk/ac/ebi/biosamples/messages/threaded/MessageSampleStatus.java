package uk.ac.ebi.biosamples.messages.threaded;

import java.util.concurrent.atomic.AtomicBoolean;

public class MessageSampleStatus<S> {

	public final AtomicBoolean storedInRepository = new AtomicBoolean(false);
	
	public final S sample;
	
	private MessageSampleStatus(S sample) {
		this.sample = sample;
	}
	
	public static <S> MessageSampleStatus<S> build(S sample) {
		if (sample == null) throw new IllegalArgumentException("sample cannot be null");
		return new MessageSampleStatus<>(sample);
	}
}
