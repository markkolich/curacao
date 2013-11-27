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

import com.kolich.curacao.annotations.Controller;
import com.kolich.curacao.annotations.methods.GET;
import com.kolich.curacao.annotations.methods.POST;
import com.kolich.curacao.annotations.parameters.RequestBody;
import com.kolich.curacao.examples.entities.FoobarGsonEntity;
import org.slf4j.Logger;

import java.util.Date;

import static org.slf4j.LoggerFactory.getLogger;

@Controller
public final class GsonExampleController {
	
	private static final Logger logger__ =
		getLogger(GsonExampleController.class);

	@GET("/api/json")
	public final FoobarGsonEntity getJson() {
		final Date d = new Date();		
		return new FoobarGsonEntity(d.toString(), d.getTime());
	}
	
	@POST("/api/json")
	public final String postJson(@RequestBody final FoobarGsonEntity entity) {
        return entity.toString();
	}

}
