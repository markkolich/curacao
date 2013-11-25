package com.kolich.curacao.examples.controllers;

import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.servlet.AsyncContext;

import com.kolich.curacao.annotations.Controller;
import com.kolich.curacao.annotations.methods.GET;
import com.kolich.curacao.examples.filters.SampleEpochModTwoFilter;

@Controller
public class FutureExampleController {
			
	@GET(value="/api/future", filter=SampleEpochModTwoFilter.class)
	public final Future<String> backToTheFuture(final AsyncContext context) {
		return new BogusRandomPausingFuture();
	}
	
	private final class BogusRandomPausingFuture implements Future<String> {
		@Override
		public boolean cancel(boolean mayInterruptIfRunning) {
			return false; // NOOP
		}
		@Override
		public boolean isCancelled() {
			return false; // NOOP
		}
		@Override
		public boolean isDone() {
			return false; // NOOP
		}
		@Override
		public String get() throws InterruptedException, ExecutionException {
			// Wait randomly between 1 and 5 seconds.
			final int wait = (new Random().nextInt(4000) + 1000);
			Thread.sleep(wait);
			return String.format("Back to the future!\n" +
				"Waited %d-milliseconds.", wait);
		}
		@Override
		public String get(final long timeout, final TimeUnit unit)
			throws InterruptedException, ExecutionException, TimeoutException {
			return get();
		}		
	}

}
