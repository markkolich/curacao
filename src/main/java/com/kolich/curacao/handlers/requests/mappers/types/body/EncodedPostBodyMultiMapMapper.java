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

package com.kolich.curacao.handlers.requests.mappers.types.body;

import static com.google.common.base.Preconditions.checkArgument;
import static java.net.URLDecoder.decode;

import java.io.UnsupportedEncodingException;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.kolich.curacao.annotations.parameters.RequestBody;

public final class EncodedPostBodyMultiMapMapper
	extends MemoryBufferingRequestBodyMapper<Multimap<String,String>> {
	
	private static final char[] DELIMITER = new char[]{'&'};

	@Override
	public final Multimap<String,String> resolveSafely(
		final RequestBody annotation, final Map<String,String> pathVars,
		final HttpServletRequest request, final HttpServletResponse response,
		final byte[] body) throws Exception {
		final String charset = getRequestCharset(request);
		return parse(StringUtils.toString(body, charset), charset);
	}

	private static final Multimap<String,String> parse(final String body,
		final String charset) throws UnsupportedEncodingException {
		final Multimap<String,String> map = LinkedHashMultimap.create();
		final StringBuffer buffer = new StringBuffer(body);
		final ParserCursor cursor = new ParserCursor(0, buffer.length());
		while(!cursor.atEnd()) {
			final Map.Entry<String,String> entry = getNextNameValuePair(buffer,
				cursor);
			if(!entry.getKey().isEmpty()) {
				map.put(decode(entry.getKey(), charset),
					decode(entry.getValue(), charset));
			}
		}
		return Multimaps.unmodifiableMultimap(map);
	}

	private static final Map.Entry<String,String> getNextNameValuePair(
		final StringBuffer buffer, final ParserCursor cursor) {

		boolean terminated = false;

		int pos = cursor.getPos();
		int indexFrom = cursor.getPos();
		int indexTo = cursor.getUpperBound();

		// Find name
		String name = null;
		while (pos < indexTo) {
			char ch = buffer.charAt(pos);
			if (ch == '=') {
				break;
			}
			if (isOneOf(ch, DELIMITER)) {
				terminated = true;
				break;
			}
			pos++;
		}

		if (pos == indexTo) {
			terminated = true;
			name = buffer.substring(indexFrom, indexTo).trim();
		} else {
			name = buffer.substring(indexFrom, pos).trim();
			pos++;
		}

		if (terminated) {
			cursor.updatePos(pos);
			return Maps.immutableEntry(name, null);
		}

		// Find value
		String value = null;
		int i1 = pos;

		boolean qouted = false;
		boolean escaped = false;
		while (pos < indexTo) {
			char ch = buffer.charAt(pos);
			if (ch == '"' && !escaped) {
				qouted = !qouted;
			}
			if (!qouted && !escaped && isOneOf(ch, DELIMITER)) {
				terminated = true;
				break;
			}
			if (escaped) {
				escaped = false;
			} else {
				escaped = qouted && ch == '\\';
			}
			pos++;
		}

		int i2 = pos;
		// Trim leading white spaces
		while (i1 < i2 && (isWhitespace(buffer.charAt(i1)))) {
			i1++;
		}
		// Trim trailing white spaces
		while ((i2 > i1) && (isWhitespace(buffer.charAt(i2 - 1)))) {
			i2--;
		}
		// Strip away quotes if necessary
		if (((i2 - i1) >= 2) && (buffer.charAt(i1) == '"')
				&& (buffer.charAt(i2 - 1) == '"')) {
			i1++;
			i2--;
		}
		value = buffer.substring(i1, i2);
		if (terminated) {
			pos++;
		}
		cursor.updatePos(pos);
		return Maps.immutableEntry(name, value);
	}
	
	private static boolean isOneOf(final char ch, final char[] chs) {
        if (chs != null) {
            for (int i = 0; i < chs.length; i++) {
                if (ch == chs[i]) {
                    return true;
                }
            }
        }
        return false;
    }
	
	private static final int CR = 13; // <US-ASCII CR, carriage return (13)>
    private static final int LF = 10; // <US-ASCII LF, linefeed (10)>
    private static final int SP = 32; // <US-ASCII SP, space (32)>
    private static final int HT = 9;  // <US-ASCII HT, horizontal-tab (9)>
	
	private static final boolean isWhitespace(final char ch) {
        return ch == SP || ch == HT || ch == CR || ch == LF;
    }
	
	private static final class ParserCursor {

	    private final int lowerBound_;
	    private final int upperBound_;
	    private int pos_;

	    public ParserCursor(final int lowerBound, final int upperBound) {
	    	checkArgument(lowerBound >= 0, "Lower bound cannot be negative.");
	    	checkArgument(lowerBound < upperBound, "Lower bound cannot " +
	            "be greater than upper bound.");
	        lowerBound_ = lowerBound;
	        upperBound_ = upperBound;
	        pos_ = lowerBound;
	    }

	    @SuppressWarnings("unused")
		public int getLowerBound() {
	        return lowerBound_;
	    }

	    public int getUpperBound() {
	        return upperBound_;
	    }

	    public int getPos() {
	        return pos_;
	    }

	    public void updatePos(int pos) {
	    	checkArgument(pos >= lowerBound_, "pos: " + pos +
	            " < lowerBound: " + lowerBound_);
	    	checkArgument(pos <= upperBound_, "pos: " + pos +
	            " > upperBound: " + upperBound_);
	        pos_ = pos;
	    }

	    public boolean atEnd() {
	        return pos_ >= upperBound_;
	    }

	}

}
