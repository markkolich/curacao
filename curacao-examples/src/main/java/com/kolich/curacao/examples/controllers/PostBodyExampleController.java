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

import com.google.common.collect.Multimap;
import com.kolich.curacao.annotations.Controller;
import com.kolich.curacao.annotations.methods.RequestMapping;
import com.kolich.curacao.annotations.methods.RequestMapping.Method;
import com.kolich.curacao.annotations.parameters.RequestBody;

import java.util.Arrays;
import java.util.Map;

import static org.apache.commons.io.IOUtils.LINE_SEPARATOR_UNIX;

@Controller
public final class PostBodyExampleController {
			
	@RequestMapping(value="^\\/api\\/postbody$", methods= Method.POST)
	public final String postBody(
        // The encoded POST body, parsed into a Multimap.
        @RequestBody final Multimap<String,String> post,
        // The entire POST body as a single String.
        @RequestBody final String rawBody,
        // A single parameter from the POST body.
        @RequestBody("data") final String data,
        // Raw body as a byte[] array too.
        @RequestBody byte[] body) {
		final StringBuilder sb = new StringBuilder();
        for (final Map.Entry<String,String> entry : post.entries()) {
			sb.append(entry.getKey() + " -> " + entry.getValue() +
				LINE_SEPARATOR_UNIX);
		}
        sb.append("-------\n").append(rawBody).append("\n");
        sb.append("-------\n").append(data).append("\n");
        sb.append("-------\n").append(Arrays.toString(body));
		return sb.toString();
	}

}
