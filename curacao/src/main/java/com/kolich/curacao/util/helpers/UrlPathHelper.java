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

package com.kolich.curacao.util.helpers;

import com.kolich.curacao.CuracaoContext;

import javax.servlet.http.HttpServletRequest;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

public final class UrlPathHelper {
	
	// Cannot instantiate.
	private UrlPathHelper() {}
	private static class LazyHolder {
		private static final UrlPathHelper instance__ = new UrlPathHelper();
	}
	public static final UrlPathHelper getInstance() {
		return LazyHolder.instance__;
	}

    public final String getPathWithinApplication(final CuracaoContext ctx) {
        return getPathWithinApplication(ctx.request_, ctx.servletCtx_.getContextPath());
    }
	
	/**
	 * Return the path within the web application for the given request.
	 *
	 * @param request current HTTP request
	 * @return the path within the web application
	 */
	public final String getPathWithinApplication(final HttpServletRequest request,
                                                 final String contextPath) {
		final String requestUri = getRequestUri(request);
		final String path = getRemainingPath(requestUri, contextPath, true);
		if (path != null) {
			// Normal case: URI contains context path.
			return (isNotBlank(path) ? path : "/");
		} else {
			return requestUri;
		}
	}
	
	/**
	 * Return the request URI for the given request.
	 *
	 * <p>As the value returned by {@code request.getRequestURI()} is <i>not</i>
	 * decoded by the servlet container, this method will decode it.
	 * <p>The URI that the web container resolves <i>should</i> be correct, but some
	 * containers like JBoss/Jetty incorrectly include ";" strings like ";jsessionid"
	 * in the URI. This method cuts off such incorrect appendices.
	 *
	 * @param request current HTTP request
	 * @return the request URI
	 */
	private final String getRequestUri(final HttpServletRequest request) {
		final String uri = request.getRequestURI();
		// https://github.com/markkolich/curacao/issues/17
		// Apparently it's possible for HttpServletRequest.getRequestURI() to return null.  Looking at Jetty 9
		// source specifically, there is a case in which the Servlet container could return null although the servlet
		// spec seems to imply that returning null is invalid/impossible.  I've decided not to do anything about this
		// and just ignore the fact that the servlet container could be wrong and have bugs, and it's not
		// Curacao's job to work around those bugs by adding null checks everywhere.
		return decodeAndCleanUriString(uri);
	}
	
	/**
	 * Decode the supplied URI string and strips any extraneous portion after a ';'.
	 */
	private final String decodeAndCleanUriString(final String uri) {
		return removeJsessionid(removeSemicolonContent(uri));
	}
	
	private final String removeSemicolonContent(final String requestUri) {
		String uri = requestUri;
		int semicolonIndex = uri.indexOf(';');
		while (semicolonIndex != -1) {
			final int slashIndex = uri.indexOf('/', semicolonIndex);
			final String start = uri.substring(0, semicolonIndex);
			uri = (slashIndex != -1) ? start + uri.substring(slashIndex) : start;
			semicolonIndex = uri.indexOf(';', semicolonIndex);
		}
		return uri;
	}
	
	private final String removeJsessionid(final String requestUri) {
		String uri = requestUri;
		int startIndex = uri.indexOf(";jsessionid=");
		if (startIndex != -1) {
			final int endIndex = uri.indexOf(';', startIndex + 12);
			final String start = uri.substring(0, startIndex);
			uri = (endIndex != -1) ? start + uri.substring(endIndex) : start;
		}
		return uri;
	}
	
	/**
	 * Match the given "mapping" to the start of the "requestUri" and if there is a match return the extra part.
	 * This method is needed because the context path and the servlet path returned by the HttpServletRequest are
	 * stripped of semicolon content unlike the request URI.
	 */
	private final String getRemainingPath(final String requestUri,
                                          final String mapping,
                                          final boolean ignoreCase) {
		int index1 = 0;
		int index2 = 0;
		for ( ; (index1 < requestUri.length()) && (index2 < mapping.length()); index1++, index2++) {
			char c1 = requestUri.charAt(index1);
			char c2 = mapping.charAt(index2);
			if (c1 == ';') {
				index1 = requestUri.indexOf('/', index1);
				if (index1 == -1) {
					return null;
				}
				c1 = requestUri.charAt(index1);
			}
			if (c1 == c2) {
				continue;
			}
			if (ignoreCase && (Character.toLowerCase(c1) == Character.toLowerCase(c2))) {
				continue;
			}
			return null;
		}
		if (index2 != mapping.length()) {
			return null;
		}
		if (index1 == requestUri.length()) {
			return "";
		} else if (requestUri.charAt(index1) == ';') {
			index1 = requestUri.indexOf('/', index1);
		}
		return (index1 != -1) ? requestUri.substring(index1) : "";
	}

}
