package com.kolich.curacao.examples.controllers;

import static org.apache.commons.io.IOUtils.LINE_SEPARATOR_UNIX;

import java.util.Map;

import com.google.common.collect.Multimap;
import com.kolich.curacao.annotations.Controller;
import com.kolich.curacao.annotations.methods.POST;
import com.kolich.curacao.annotations.parameters.RequestBody;

@Controller
public final class PostBodyExampleController {
			
	@POST("/api/postbody")
	public final String postBody(@RequestBody final Multimap<String,String> post) {
		final StringBuilder sb = new StringBuilder();
		for(final Map.Entry<String,String> entry : post.entries()) {
			sb.append(entry.getKey() + " -> " + entry.getValue() +
				LINE_SEPARATOR_UNIX);
		}
		return sb.toString();
	}

}
