package uk.ac.ebi.biosamples.messages.threaded;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicMarkableReference;

public class MessageSampleStatus<S> {

	public final AtomicBoolean storedInRepository = new AtomicBoolean(false);
	public final AtomicMarkableReference<RuntimeException> hadProblem = new AtomicMarkableReference<>(null, false);
	
	public final S sample;
	
	private MessageSampleStatus(S sample) {
		this.sample = sample;
	}
	
	public static <S> MessageSampleStatus<S> build(S sample) {
		if (sample == null) throw new IllegalArgumentException("sample cannot be null");
		return new MessageSampleStatus<>(sample);
	}
}
