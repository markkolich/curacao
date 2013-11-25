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
