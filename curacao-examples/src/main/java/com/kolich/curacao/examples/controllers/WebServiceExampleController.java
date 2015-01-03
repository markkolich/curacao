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

package com.kolich.curacao.examples.controllers;

import com.kolich.curacao.annotations.Controller;
import com.kolich.curacao.annotations.Injectable;
import com.kolich.curacao.annotations.methods.RequestMapping;
import com.kolich.curacao.examples.components.AsyncHttpClientComponent;
import com.ning.http.client.AsyncCompletionHandler;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.Response;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.concurrent.Future;

import static com.google.common.base.Charsets.UTF_8;
import static org.slf4j.LoggerFactory.getLogger;

@Controller
public final class WebServiceExampleController
    extends AbstractBaseExampleController {
	
	private static final Logger logger__ =
		getLogger(WebServiceExampleController.class);
	
	private final AsyncHttpClient client_;
	
	@Injectable
	public WebServiceExampleController(final AsyncHttpClientComponent client) {
		client_ = client.getClient();
	}
	
	@RequestMapping("^\\/api\\/webservice$")
	public final Future<String> callWebServiceAsync() throws IOException {
		// Use the Ning AsyncHttpClient to make a call to an external web
		// service and immediately return a Future<?> that will "complete"
		// when the AsyncHttpClient has fetched the content/URL.
		return client_.prepareGet("http://www.google.com/robots.txt")
			.execute(new AsyncCompletionHandler<String>() {
				@Override
				public String onCompleted(final Response response) throws Exception {
					return response.getResponseBody(UTF_8.toString());
				}
				@Override
				public void onThrowable(Throwable t) {
					logger__.error("Web-service request failed miserably.", t);
				}
			});
	}

}
