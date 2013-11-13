package com.kolich.curacao.examples.controllers;

import static com.google.common.base.Charsets.UTF_8;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.util.concurrent.Future;

import org.slf4j.Logger;

import com.kolich.curacao.annotations.Controller;
import com.kolich.curacao.annotations.methods.GET;
import com.ning.http.client.AsyncCompletionHandler;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.Response;

@Controller
public final class WebServiceExampleController {
	
	private static final Logger logger__ =
		getLogger(WebServiceExampleController.class);
	
	private static final AsyncHttpClient asyncHttpClient__ =
		new AsyncHttpClient();

	@GET("/api/webservice")
	public final Future<String> callWebServiceAsync() throws IOException {
		// Use the Ning AsyncHttpClient to make a call to an external web
		// service and immediately return a Future<?> that will "complete"
		// when the AsyncHttpClient has fetched the page.
		return asyncHttpClient__.prepareGet("http://www.google.com/robots.txt")
			.execute(new AsyncCompletionHandler<String>() {
				@Override
				public String onCompleted(final Response response) throws Exception {
					return response.getResponseBody(UTF_8.toString());
				}
				@Override
				public void onThrowable(Throwable t) {
					logger__.error("Web-service request failed " +
						"miserably.", t);
				}
			});
	}

}
