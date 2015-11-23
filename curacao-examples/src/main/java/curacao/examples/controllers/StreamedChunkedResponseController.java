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

package curacao.examples.controllers;

import curacao.annotations.Controller;
import curacao.annotations.RequestMapping;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import javax.servlet.AsyncContext;
import javax.servlet.http.HttpServletResponse;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Date;

import static com.google.common.base.Charsets.UTF_8;
import static com.google.common.net.MediaType.PLAIN_TEXT_UTF_8;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static org.apache.commons.io.IOUtils.LINE_SEPARATOR_UNIX;
import static org.slf4j.LoggerFactory.getLogger;

@Controller
public final class StreamedChunkedResponseController {
	
	private static final Logger logger__ =
		getLogger(StreamedChunkedResponseController.class);
	
	private static final int CHUNKS_TO_SEND = 8;
	
	private static final String CHUNKED_RESPONSE_PADDING =
		StringUtils.repeat(" ", 2048); 
	
	@RequestMapping("^\\/api\\/chunked$")
	public final void streamChunked(final AsyncContext context,
                                    final HttpServletResponse response) {
		// Tell the Servlet container the request was successful
		// and that the client/browser should expect some chunked
		// plain text data back.
		response.setStatus(SC_OK);
		response.setContentType(PLAIN_TEXT_UTF_8.toString());
		// Grab a new writer, actually OutputStreamWriter, and write
		// the chunked data to the output stream.
		try(final Writer writer = new OutputStreamWriter(
			response.getOutputStream(), UTF_8.toString())) {
			// It's unclear why this is important, but in order for the
			// browser to show the data chunks "streamed in" from the
			// server side (this Servlet) as they are delivered, we
			// have to send 2KB of empty characters (spaces) first and
			// then the browser will automatically refresh/update the
			// page as bytes are delivered.  If this is omitted, then
			// the browser will wait for all bytes to be delivered in
			// the chunked response before it attempts to "render"
			// something visible to the user.  Seems like this might
			// have something to do with the "server side buffer" that
			// needs flushing before the browser will show anything:
			// http://stackoverflow.com/q/13565952
			writer.write(CHUNKED_RESPONSE_PADDING);
			writer.write(LINE_SEPARATOR_UNIX);
			// For X in N, send some data followed by a new line and
			// then flush the stream.
			for (int i = 1; i <= CHUNKS_TO_SEND; i++) {
				writer.write(String.format("Chunk %d of %d: ", i,
					CHUNKS_TO_SEND));
				writer.write(new Date().toString() + LINE_SEPARATOR_UNIX);
				writer.flush();
				// Wait for almost a second to simulate "work" going
				// on behind the scenes that's actually streaming down bytes.
				Thread.sleep(700L);
			}
		} catch (Exception e) {
			logger__.warn("Unexpected exception occurred while sending " +
				"data to client.", e);
		} finally {
			context.complete();
		}
	}

}
