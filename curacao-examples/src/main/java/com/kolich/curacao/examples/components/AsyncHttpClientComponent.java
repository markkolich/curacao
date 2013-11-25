package com.kolich.curacao.examples.components;

import static org.slf4j.LoggerFactory.getLogger;

import org.slf4j.Logger;

import com.kolich.curacao.annotations.Component;
import com.kolich.curacao.handlers.components.CuracaoComponent;
import com.ning.http.client.AsyncHttpClient;

@Component
public final class AsyncHttpClientComponent implements CuracaoComponent {
	
	private static final Logger logger__ = 
		getLogger(AsyncHttpClientComponent.class);
	
	private final AsyncHttpClient asyncHttpClient_;
	
	public AsyncHttpClientComponent() {
		asyncHttpClient_ = new AsyncHttpClient();
	}
	
	public AsyncHttpClient getClient() {
		return asyncHttpClient_;
	}

	@Override
	public void initialize() throws Exception {
		logger__.info("Inside AsyncHttpClientComponent initialize!");
	}

	@Override
	public void destroy() throws Exception {
		logger__.info("Inside AsyncHttpClientComponent destroy!");
		asyncHttpClient_.close();
	}
	
}
