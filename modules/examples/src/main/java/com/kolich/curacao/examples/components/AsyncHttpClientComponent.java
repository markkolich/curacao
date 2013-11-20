package com.kolich.curacao.examples.components;

import static org.slf4j.LoggerFactory.getLogger;

import org.slf4j.Logger;

import com.kolich.curacao.annotations.Component;
import com.kolich.curacao.handlers.components.CuracaoComponent;

@Component
public final class AsyncHttpClientComponent implements CuracaoComponent {
	
	private static final Logger logger__ = 
		getLogger(AsyncHttpClientComponent.class);

	@Override
	public void initialize() throws Exception {
		logger__.info("Inside AsyncHttpClientComponent initialize!");
	}

	@Override
	public void destroy() throws Exception {
		logger__.info("Inside AsyncHttpClientComponent destroy!");
	}
	
}
