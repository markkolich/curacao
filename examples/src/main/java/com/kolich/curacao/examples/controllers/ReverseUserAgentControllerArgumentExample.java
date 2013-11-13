package com.kolich.curacao.examples.controllers;

import com.kolich.curacao.annotations.Controller;
import com.kolich.curacao.annotations.methods.GET;
import com.kolich.curacao.annotations.parameters.convenience.UserAgent;
import com.kolich.curacao.examples.entities.ReverseUserAgent;

@Controller
public final class ReverseUserAgentControllerArgumentExample {
		
	@GET("/api/reverse")
	public final String reverseUserAgent(@UserAgent final String userAgent,
		final ReverseUserAgent reverse) {
		return String.format("%s\n%s", userAgent, reverse.toString());
	}

}
