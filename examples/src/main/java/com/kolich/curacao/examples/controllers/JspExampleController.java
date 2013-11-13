package com.kolich.curacao.examples.controllers;

import javax.servlet.AsyncContext;

import com.kolich.curacao.annotations.Controller;
import com.kolich.curacao.annotations.methods.GET;

@Controller
public final class JspExampleController {

	@GET("/api/jsp")
	public final void dispatchToJsp(final AsyncContext context) {
		context.dispatch("/WEB-INF/jsp/demo.jsp");
	}

}
