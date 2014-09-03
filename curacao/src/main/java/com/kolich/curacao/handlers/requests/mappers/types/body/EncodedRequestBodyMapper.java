/**
 * Copyright (c) 2014 Mark S. Kolich
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

import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.kolich.curacao.annotations.parameters.RequestBody;
import org.apache.commons.lang3.StringUtils;

import java.util.Map;

import static com.google.common.base.Preconditions.checkArgument;
import static java.net.URLDecoder.decode;

public abstract class EncodedRequestBodyMapper<T>
	extends RequestBodyAsCharsetAwareStringMapper<T> {

	private static final char DELIMITER = '&';
	private static final char KEY_VALUE_EQUALS = '=';
	private static final char VALUE_DOUBLE_QUOTE = '"';

    @Override
    public final T resolveWithStringAndEncoding(final RequestBody annotation,
                                                final String s,
                                                final String encoding) throws Exception {
        return resolveWithMultimap(annotation, parse(s, encoding));
    }

    public abstract T resolveWithMultimap(final RequestBody annotation,
                                          final Multimap<String,String> map) throws Exception;

    private static final Multimap<String,String> parse(final String body,
                                                       final String encodingCharset) throws Exception {
		final ImmutableMultimap.Builder<String,String> result = ImmutableListMultimap.builder();
        // <https://github.com/markkolich/curacao/issues/12>
        // Only bother parsing the POST body if there's actually something there to parse.
        if(!StringUtils.isEmpty(body)) {
            final StringBuffer buffer = new StringBuffer(body);
            final Cursor cursor = new Cursor(0, buffer.length());
            while(!cursor.atEnd()) {
                final Map.Entry<String,String> entry =
                    getNextNameValuePair(buffer, cursor);
                if(!entry.getKey().isEmpty()) {
                    result.put(decode(entry.getKey(), encodingCharset),
                        decode(entry.getValue(), encodingCharset));
                }
            }
        }
        return result.build();
	}

	private static final Map.Entry<String,String> getNextNameValuePair(final StringBuffer buffer,
                                                                       final Cursor cursor) {
		boolean terminated = false;

		int pos = cursor.getPosition(),
			indexFrom = cursor.getPosition(),
			indexTo = cursor.getUpperBound();

		// Find name
		String name = null;
		while(pos < indexTo) {
			final char ch = buffer.charAt(pos);
			if(ch == KEY_VALUE_EQUALS) {
				break;
			}
			if(ch == DELIMITER) {
				terminated = true;
				break;
			}
			pos++;
		}

		if(pos == indexTo) {
			terminated = true;
			name = buffer.substring(indexFrom, indexTo).trim();
		} else {
			name = buffer.substring(indexFrom, pos).trim();
			pos++;
		}

		if(terminated) {
			cursor.updatePosition(pos);
			return Maps.immutableEntry(name, null);
		}

		// Find value
		String value = null;
		int i1 = pos;

		boolean quoted = false, escaped = false;
		while(pos < indexTo) {
			char ch = buffer.charAt(pos);
			if (ch == VALUE_DOUBLE_QUOTE && !escaped) {
				quoted = !quoted;
			}
			if (!quoted && !escaped && ch == DELIMITER) {
				terminated = true;
				break;
			}
			if (escaped) {
				escaped = false;
			} else {
				escaped = quoted && ch == '\\';
			}
			pos++;
		}

		int i2 = pos;
		// Trim leading white space
		while(i1 < i2 && (Whitespace.isWhitespace(buffer.charAt(i1)))) {
			i1++;
		}
		// Trim trailing white space
		while((i2 > i1) && (Whitespace.isWhitespace(buffer.charAt(i2 - 1)))) {
			i2--;
		}
		// Strip away quotes if necessary
		if(((i2 - i1) >= 2) && (buffer.charAt(i1) == '"')
			&& (buffer.charAt(i2 - 1) == '"')) {
			i1++;
			i2--;
		}
		
		value = buffer.substring(i1, i2);
		if(terminated) {
			pos++;
		}
		cursor.updatePosition(pos);
		return Maps.immutableEntry(name, value);
	}

    private static final class Whitespace {
		
		private static final int CR = 13; // <US-ASCII CR, carriage return (13)>
	    private static final int LF = 10; // <US-ASCII LF, linefeed (10)>
	    private static final int SP = 32; // <US-ASCII SP, space (32)>
	    private static final int HT = 9;  // <US-ASCII HT, horizontal-tab (9)>
	    
		private static final boolean isWhitespace(final char ch) {
	        return ch == SP || ch == HT || ch == CR || ch == LF;
	    }
		
	}
	
	private static final class Cursor {

	    private final int lowerBound_;
	    private final int upperBound_;
	    private int position_;

	    public Cursor(final int lowerBound, final int upperBound) {
	    	checkArgument(lowerBound >= 0, "Lower bound cannot be negative.");
	    	checkArgument(lowerBound < upperBound, "Lower bound cannot " +
	            "be greater than upper bound.");
	        lowerBound_ = lowerBound;
	        upperBound_ = upperBound;
	        position_ = lowerBound;
	    }

	    @SuppressWarnings("unused")
		public int getLowerBound() {
	        return lowerBound_;
	    }

	    public int getUpperBound() {
	        return upperBound_;
	    }

	    public int getPosition() {
	        return position_;
	    }

	    public void updatePosition(final int pos) {
	    	checkArgument(pos >= lowerBound_, "pos: " + pos +
	            " < lowerBound: " + lowerBound_);
	    	checkArgument(pos <= upperBound_, "pos: " + pos +
	            " > upperBound: " + upperBound_);
	        position_ = pos;
	    }

	    public boolean atEnd() {
	        return position_ >= upperBound_;
	    }

	}

}
