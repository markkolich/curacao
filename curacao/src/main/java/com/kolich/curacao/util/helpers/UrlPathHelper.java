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

package com.kolich.curacao.util.helpers;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import javax.servlet.http.HttpServletRequest;

public final class UrlPathHelper {
	
	// Cannot instantiate.
	private UrlPathHelper() {}	
	private static class LazyHolder {
		private static final UrlPathHelper instance__ = new UrlPathHelper();
	}	
	public static final UrlPathHelper getInstance() {
		return LazyHolder.instance__;
	}
	
	/**
	 * Return the path within the web application for the given request.
	 * @param request current HTTP request
	 * @return the path within the web application
	 */
	public final String getPathWithinApplication(
		final HttpServletRequest request) {
		final String contextPath = getContextPath(request);
		final String requestUri = getRequestUri(request);
		final String path = getRemainingPath(requestUri, contextPath, true);
		if (path != null) {
			// Normal case: URI contains context path.
			return (isNotBlank(path) ? path : "/");
		} else {
			return requestUri;
		}
	}
	
	private final String getContextPath(final HttpServletRequest request) {
		String contextPath = request.getContextPath();
		if ("/".equals(contextPath) || contextPath == null) {
			// Invalid case, but happens for includes on Jetty: silently adapt it.
			contextPath = "";
		}
		return contextPath;
	}
	
	/**
	 * Return the request URI for the given request, detecting an include request
	 * URL if called within a RequestDispatcher include.
	 * <p>As the value returned by {@code request.getRequestURI()} is <i>not</i>
	 * decoded by the servlet container, this method will decode it.
	 * <p>The URI that the web container resolves <i>should</i> be correct, but some
	 * containers like JBoss/Jetty incorrectly include ";" strings like ";jsessionid"
	 * in the URI. This method cuts off such incorrect appendices.
	 * @param request current HTTP request
	 * @return the request URI
	 */
	private final String getRequestUri(final HttpServletRequest request) {
		final String uri = request.getRequestURI();
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
			int slashIndex = uri.indexOf('/', semicolonIndex);
			String start = uri.substring(0, semicolonIndex);
			uri = (slashIndex != -1) ? start + uri.substring(slashIndex) : start;
			semicolonIndex = uri.indexOf(';', semicolonIndex);
		}
		return uri;
	}
	
	private final String removeJsessionid(final String requestUri) {
		String uri = requestUri;
		int startIndex = uri.indexOf(";jsessionid=");
		if (startIndex != -1) {
			int endIndex = uri.indexOf(';', startIndex + 12);
			String start = uri.substring(0, startIndex);
			uri = (endIndex != -1) ? start + uri.substring(endIndex) : start;
		}
		return uri;
	}
	
	/**
	 * Match the given "mapping" to the start of the "requestUri" and if there
	 * is a match return the extra part. This method is needed because the
	 * context path and the servlet path returned by the HttpServletRequest are
	 * stripped of semicolon content unlike the requesUri.
	 */
	private final String getRemainingPath(final String requestUri,
		final String mapping, final boolean ignoreCase) {
		int index1 = 0;
		int index2 = 0;
		for ( ; (index1 < requestUri.length()) && (index2 < mapping.length());
            index1++, index2++) {
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
			if (ignoreCase && (Character.toLowerCase(c1) ==
                   Character.toLowerCase(c2))) {
				continue;
			}
			return null;
		}
		if (index2 != mapping.length()) {
			return null;
		}
		if (index1 == requestUri.length()) {
			return "";
		}
		else if (requestUri.charAt(index1) == ';') {
			index1 = requestUri.indexOf('/', index1);
		}
		return (index1 != -1) ? requestUri.substring(index1) : "";
	}

}
