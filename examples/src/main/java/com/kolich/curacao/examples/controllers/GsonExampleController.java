/**
 * Copyright (c) 2013 Mark S. Kolich
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

import static org.slf4j.LoggerFactory.getLogger;

import java.util.Date;

import org.slf4j.Logger;

import com.kolich.common.date.ISO8601DateFormat;
import com.kolich.curacao.annotations.Controller;
import com.kolich.curacao.annotations.methods.GET;
import com.kolich.curacao.annotations.methods.POST;
import com.kolich.curacao.annotations.parameters.Path;
import com.kolich.curacao.annotations.parameters.RequestBody;
import com.kolich.curacao.examples.entities.FoobarGsonEntity;

@Controller
public final class GsonExampleController {
	
	private static final Logger logger__ =
		getLogger(GsonExampleController.class);
	
	@GET("/api/json")
	public final FoobarGsonEntity getSomeJson() {
		final Date d = new Date();		
		return new FoobarGsonEntity(ISO8601DateFormat.format(d), d.getTime());
	}
	
	@POST("/api/json/{path}")
	public final String postSomeJson(@RequestBody final String body,
		@Path("path") final String path) {
		logger__.info("body: " + body);
		return body + path;
	}

}
