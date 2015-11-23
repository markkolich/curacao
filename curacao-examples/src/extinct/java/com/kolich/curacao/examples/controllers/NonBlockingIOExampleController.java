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

import static java.nio.channels.Channels.newChannel;
import static org.apache.commons.codec.binary.StringUtils.getBytesUtf8;
import static org.apache.commons.io.IOUtils.LINE_SEPARATOR_UNIX;
import static org.apache.commons.io.IOUtils.closeQuietly;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

import javax.servlet.AsyncContext;
import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;

import com.kolich.common.functional.option.None;
import com.kolich.common.functional.option.Option;
import com.kolich.common.functional.option.Some;
import curacao.annotations.Controller;
import curacao.annotations.methods.PUT;

@Controller
public final class NonBlockingIOExampleController {
	
	private static final Logger logger__ =
		getLogger(NonBlockingIOExampleController.class);
	
	// curl --noproxy localhost -vvv -k -X PUT -d "@enormous.out" --limit-rate 1K http://localhost:8080/api/nonblocking
	
	// curl --noproxy localhost -vvv -k -X POST -d "here are some bytes" http://localhost:8080/api/nonblocking
	
	// http://git.eclipse.org/c/jetty/org.eclipse.jetty.project.git/tree/jetty-servlets/src/main/java/org/eclipse/jetty/servlets/DataRateLimitedServlet.java?h=jetty-9.1

	@PUT("/api/nonblocking")
	public final void nonblocking(final AsyncContext context,
		final HttpServletRequest request, final HttpServletResponse response,
		final ServletInputStream input, final ServletOutputStream output) throws Exception {
		
		final Queue<Option<String>> queue = new ConcurrentLinkedQueue<Option<String>>();
		
		final WriteListener writer = new StupidWriteListener(context, queue,
			output);
		
		final ReadListener reader = new StupidReadListener(queue, input,
			output, writer, request.getContentLength());
		
		// Producer
		input.setReadListener(reader);
		logger__.info("Tomcat, is input ready?: " + input.isReady());
		reader.onDataAvailable();
		
		// Consumer
		output.setWriteListener(writer);		
		logger__.info("Tomcat, is input ready?: " + output.isReady());
		writer.onWritePossible();
		
	}
	
	private class StupidReadListener implements ReadListener, Runnable {
		private final Queue<Option<String>> queue_;
		private final ServletInputStream input_;
		private final ServletOutputStream output_;
		private final WriteListener writer_;
		private final ServletInputStreamBuffer buffer_;
		public StupidReadListener(final Queue<Option<String>> queue,
			final ServletInputStream input,
			final ServletOutputStream output,
			final WriteListener writer,
			final int contentLength) {
			queue_ = queue;
			input_ = input;
			output_ = output;
			writer_ = writer;
			buffer_ = new StaticByteBuffer(contentLength);
		}
		@Override
		public void onDataAvailable() throws IOException {
			buffer_.fill(input_);
			final String msg = "Read " + buffer_.getTotalBytesRead() +
				"-bytes!";
			logger__.info(msg);
			queue_.add(Some.some(msg));
			//logger__.info("Queue has " + queue.size() + "-elements.");
			/*if(output_.isReady()) {
				logger__.info("onDataAvailable: Output is ready!");
				writer_.onWritePossible();
			} else {
				logger__.info("onDataAvailable: Output is NOT ready!");
			}*/
		}
		@Override
		public void onAllDataRead() throws IOException {
			if(input_.isFinished()) {
				queue_.add(None.<String>none());
				logger__.info("All data is read! Added NONE to queue.");
			}
			if(output_.isReady()) {
				logger__.info("onDataAvailable: Output is ready!");
				writer_.onWritePossible();
			} else {
				logger__.info("onDataAvailable: Output is NOT ready!");
			}
		}
		@Override
		public void onError(Throwable t) {				
			logger__.error("Failure while reading input stream.", t);
		}
		@Override
		public void run() {
			try {
				onDataAvailable();
			} catch (IOException e) {
				logger__.error("Failed miserably.", e);
			}
		}
	}
	
	private class StupidWriteListener implements WriteListener, Runnable {		
		private final AsyncContext context_;
		private final Queue<Option<String>> queue_;
		private final ServletOutputStream output_;
		public StupidWriteListener(final AsyncContext context,
			final Queue<Option<String>> queue,
			final ServletOutputStream output) {
			context_ = context;
			queue_ = queue;
			output_ = output;
		}
		@Override
		public void onWritePossible() throws IOException {
			if(!output_.isReady()) {
				return;
			}
			logger__.info("onWritePossible(): Output is ready!!!");			
			Option<String> o = null;
			while((o = queue_.poll()) != null && o.isSome()) {
				logger__.info("onWritePossible(): Got SOME: " + o.get());
				output_.write(getBytesUtf8(o.get() + LINE_SEPARATOR_UNIX));
			}
			if(output_.isReady()) {
				output_.flush();
			}
			if(o != null && o.isNone()) {
				logger__.info("onWritePossible(): Got NONE... " +
					"completing context!");
				context_.complete();
			}
		}
		@Override
		public void onError(Throwable t) {
			logger__.error("Failure in write listener.", t);
		}
		@Override
		public void run() {
			try {
				onWritePossible();
			} catch (IOException e) {
				logger__.error("Failed miserably.", e);
			}
		}
	}
	
	interface ServletInputStreamBuffer {
		public static final int EOF = -1;
		public void fill(final ServletInputStream input) throws IOException;
		public int getTotalBytesRead();
		public byte[] getBuffer();
	}
	
	static final class StaticByteBuffer implements ServletInputStreamBuffer {
		private final byte[] bytes_;
		private final ByteBuffer buffer_;
		private final AtomicInteger total_;
		public StaticByteBuffer(final int contentLength) {
			bytes_ = new byte[contentLength];
			buffer_ = ByteBuffer.wrap(bytes_);
			total_ = new AtomicInteger(0);
		}
		@Override
		public void fill(final ServletInputStream input) throws IOException {
			ReadableByteChannel readable = null;
			try {
				readable = newChannel(input);
				int read = 0;
				while(input.isReady() && (read = readable.read(buffer_)) != EOF) {
					total_.addAndGet(read);
				}
			} finally {
				closeQuietly(readable);
			}
		}
		@Override
		public byte[] getBuffer() {
			return bytes_;
		}
		@Override
		public int getTotalBytesRead() {
			return total_.get();
		}
	}
	
	static final class DynamicBuffer implements ServletInputStreamBuffer {
		private final ByteArrayOutputStream baos_;
		private final AtomicInteger total_;
		public DynamicBuffer() {
			baos_ = new ByteArrayOutputStream();
			total_ = new AtomicInteger(0);
		}
		@Override
		public void fill(final ServletInputStream input) throws IOException {
			final byte[] buffer = new byte[4096];
			int read = 0;
			while(input.isReady() && (read = input.read(buffer)) != EOF) {
				baos_.write(buffer, 0, read);
				total_.addAndGet(read);
			}
		}
		@Override
		public byte[] getBuffer() {
			return baos_.toByteArray();
		}
		@Override
		public int getTotalBytesRead() {
			return total_.get();
		}
	}
	
	static final class ByteBufferServletInputStream extends ServletInputStream {

        private ByteBuffer buffer_;
        private ServletInputStream delegate_;
        
        public ByteBufferServletInputStream(final ServletInputStream delegate,
        	final int size) {
            delegate_ = delegate;
            buffer_ = ByteBuffer.allocateDirect(size);
            buffer_.mark();
        }
        
        public ByteBufferServletInputStream(final ServletInputStream delegate) {
        	this(delegate, 4096);
        }
        
        @Override
		public int available() throws IOException {
            return delegate_.available();
        }

        @Override
		public void close() throws IOException {
            delegate_.close();
        }

        @Override
		public void mark(int readlimit) {
            delegate_.mark(readlimit);
        }

        @Override
		public boolean markSupported() {
            return delegate_.markSupported();
        }

        @Override
		public void reset() throws IOException {
            delegate_.reset();
        }

        @Override
		public long skip(long n) throws IOException {
            return delegate_.skip(n);
        }

        @Override
        public int read() throws IOException {
            int b = delegate_.read();
            if (!bufferIsFull()) {
                buffer_.put((byte)b);
            }
            return b;
        }
        
        @Override
        public int read(byte[] b) throws IOException {
            int n = delegate_.read(b);
            fill(b, 0, n);
            return n;
        }
        
        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            int n = delegate_.read(b, off, len);
            fill(b, off, n);
            return n;
        }
        
        @Override
        public int readLine(byte[] b, int off, int len) throws IOException {
            int n = delegate_.readLine(b, off, len);
            fill(b, off, n);
            return n;
        }
        
        void fill(byte[] b, int off, int len) {
            if (len < 0) return;
            if (!bufferIsFull()) {
                int m = Math.min(len, buffer_.capacity()-buffer_.position());
                buffer_.put(b, off, m);
            }
        }
        
        boolean bufferIsFull() {
            return buffer_.position() == buffer_.capacity();
        }
        
        public byte[] getData() {
            if (bufferIsFull()) {
                return buffer_.array();
            }            
            byte[] data = new byte[buffer_.position()];
            ((ByteBuffer)buffer_.duplicate().reset()).get(data);
            return data;
        }
        
        public byte[] array() {
        	return buffer_.array();
        }
        
        public void dispose() {
            buffer_ = null;
            delegate_ = null;
        }

		@Override
		public boolean isFinished() {
			return delegate_.isFinished();
		}

		@Override
		public boolean isReady() {
			return delegate_.isReady();
		}

		@Override
		public void setReadListener(ReadListener readListener) {
			delegate_.setReadListener(readListener);
		}
		
    }

}
