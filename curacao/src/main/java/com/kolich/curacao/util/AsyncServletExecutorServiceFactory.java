/**
 * Copyright (c) 2015 Mark S. Kolich
 * http://mark.koli.ch
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package com.kolich.curacao.util;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

import java.util.concurrent.ExecutorService;

import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.Thread.MAX_PRIORITY;
import static java.lang.Thread.MIN_PRIORITY;
import static java.util.concurrent.Executors.newCachedThreadPool;
import static java.util.concurrent.Executors.newFixedThreadPool;

public final class AsyncServletExecutorServiceFactory {
		
	private final int size_;
	
	private String threadNameFormat_ = null;
	private Boolean useDaemon_ = null;
	private Integer priority_ = null;
	
	public AsyncServletExecutorServiceFactory(final int size) {
		size_ = size;
	}
	
	public AsyncServletExecutorServiceFactory setThreadNameFormat(final String threadNameFormat) {
		threadNameFormat_ = threadNameFormat;
		return this;
	}
	
	public AsyncServletExecutorServiceFactory setPriority(final int priority) {
		checkArgument(priority >= MIN_PRIORITY, "Thread priority (%s) must be >= %s", priority, MIN_PRIORITY);
		checkArgument(priority <= MAX_PRIORITY, "Thread priority (%s) must be <= %s", priority, MAX_PRIORITY);
		priority_ = priority;
		return this;
	}
	
	public AsyncServletExecutorServiceFactory setDaemon(final Boolean useDaemon) {
		useDaemon_ = useDaemon;
		return this;
	}
	
	public final ExecutorService build() {
		final ThreadFactoryBuilder builder = new ThreadFactoryBuilder();
		if (threadNameFormat_ != null) {
			builder.setNameFormat(threadNameFormat_);
		}
		if (priority_ != null) {
			builder.setPriority(priority_);
		}
		if (useDaemon_ != null) {
			builder.setDaemon(useDaemon_);
		}
		return (size_ > 0) ?
			// Fixed sized thread pool (no more than N-threads).
			newFixedThreadPool(size_, builder.build()) :
			// Unbounded thread pool, will grow as needed.
			newCachedThreadPool(builder.build());
	}
	
	public static ExecutorService createNewService(final int size,
												   final String threadNameFormat) {
		return new AsyncServletExecutorServiceFactory(size)
			.setDaemon(true)
			.setPriority(MAX_PRIORITY)
			.setThreadNameFormat(threadNameFormat)
			.build();
	}
	
	public static ListeningExecutorService createNewListeningService(final int size,
																	 final String threadNameFormat) {
		return new SafeListeningExecutorServiceDecorator(createNewService(size, threadNameFormat));
	}

}

