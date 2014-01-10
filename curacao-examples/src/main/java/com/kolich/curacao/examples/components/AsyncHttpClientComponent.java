/**
 * Copyright (c) 2014 Mark S. Kolich
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

package com.kolich.curacao.examples.components;

import com.kolich.curacao.annotations.Component;
import com.kolich.curacao.handlers.components.ComponentDestroyable;
import com.ning.http.client.AsyncHttpClient;
import org.slf4j.Logger;

import javax.servlet.ServletContext;

import static org.slf4j.LoggerFactory.getLogger;

@Component
public final class AsyncHttpClientComponent implements ComponentDestroyable {
	
	private static final Logger logger__ = 
		getLogger(AsyncHttpClientComponent.class);
	
	private final AsyncHttpClient asyncHttpClient_;
	
	public AsyncHttpClientComponent() {
		asyncHttpClient_ = new AsyncHttpClient();
	}
	
	public final AsyncHttpClient getClient() {
		return asyncHttpClient_;
	}

	@Override
	public final void destroy(final ServletContext context) throws Exception {
		logger__.info("Inside of AsyncHttpClientComponent destroy.");
		asyncHttpClient_.close();
	}
	
}
