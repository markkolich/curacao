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

package com.kolich.curacao.handlers.requests.mappers.types;

import static com.google.common.net.HttpHeaders.ACCEPT;
import static com.google.common.net.HttpHeaders.ACCEPT_CHARSET;
import static com.google.common.net.HttpHeaders.ACCEPT_ENCODING;
import static com.google.common.net.HttpHeaders.ACCEPT_LANGUAGE;
import static com.google.common.net.HttpHeaders.AUTHORIZATION;
import static com.google.common.net.HttpHeaders.CONNECTION;
import static com.google.common.net.HttpHeaders.CONTENT_TYPE;
import static com.google.common.net.HttpHeaders.COOKIE;
import static com.google.common.net.HttpHeaders.DATE;
import static com.google.common.net.HttpHeaders.HOST;
import static com.google.common.net.HttpHeaders.IF_MATCH;
import static com.google.common.net.HttpHeaders.IF_MODIFIED_SINCE;
import static com.google.common.net.HttpHeaders.USER_AGENT;
import static com.google.common.net.HttpHeaders.VIA;

import java.lang.annotation.Annotation;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.kolich.curacao.annotations.parameters.Extension;
import com.kolich.curacao.annotations.parameters.Header;
import com.kolich.curacao.annotations.parameters.Path;
import com.kolich.curacao.annotations.parameters.Query;
import com.kolich.curacao.annotations.parameters.RequestUri;
import com.kolich.curacao.annotations.parameters.convenience.Accept;
import com.kolich.curacao.annotations.parameters.convenience.AcceptCharset;
import com.kolich.curacao.annotations.parameters.convenience.AcceptEncoding;
import com.kolich.curacao.annotations.parameters.convenience.AcceptLanguage;
import com.kolich.curacao.annotations.parameters.convenience.Authorization;
import com.kolich.curacao.annotations.parameters.convenience.Connection;
import com.kolich.curacao.annotations.parameters.convenience.ContentType;
import com.kolich.curacao.annotations.parameters.convenience.Cookie;
import com.kolich.curacao.annotations.parameters.convenience.Date;
import com.kolich.curacao.annotations.parameters.convenience.Host;
import com.kolich.curacao.annotations.parameters.convenience.IfMatch;
import com.kolich.curacao.annotations.parameters.convenience.IfModifiedSince;
import com.kolich.curacao.annotations.parameters.convenience.UserAgent;
import com.kolich.curacao.annotations.parameters.convenience.Via;
import com.kolich.curacao.handlers.requests.mappers.ControllerMethodArgumentMapper;

public final class StringMapper
	extends ControllerMethodArgumentMapper<String> {

	@Override
	public final String resolve(final Annotation annotation,
		final Map<String,String> pathVars, final HttpServletRequest request,
		final HttpServletResponse response) throws Exception {
		final String requestUri = request.getRequestURI();
		String result = null;
		if(annotation instanceof Accept) {
			result = request.getHeader(ACCEPT);
		} else if(annotation instanceof AcceptCharset) {
			result = request.getHeader(ACCEPT_CHARSET);
		} else if(annotation instanceof AcceptEncoding) {
			result = request.getHeader(ACCEPT_ENCODING);
		} else if(annotation instanceof AcceptLanguage) {
			result = request.getHeader(ACCEPT_LANGUAGE);
		} else if(annotation instanceof Authorization) {
			result = request.getHeader(AUTHORIZATION);
		} else if(annotation instanceof Connection) {
			result = request.getHeader(CONNECTION);
		} else if(annotation instanceof ContentType) {
			result = request.getHeader(CONTENT_TYPE);
		} else if(annotation instanceof Cookie) {
			result = request.getHeader(COOKIE);
		} else if(annotation instanceof Date) {
			result = request.getHeader(DATE);
		} else if(annotation instanceof Host) {
			result = request.getHeader(HOST);
		} else if(annotation instanceof IfMatch) {
			result = request.getHeader(IF_MATCH);
		} else if(annotation instanceof IfModifiedSince) {
			result = request.getHeader(IF_MODIFIED_SINCE);
		} else if(annotation instanceof UserAgent) {
			result = request.getHeader(USER_AGENT);
		} else if(annotation instanceof Via) {
			result = request.getHeader(VIA);
		} else if(annotation instanceof Query) {
			result = request.getParameter(((Query)annotation).value());
		} else if(annotation instanceof Path) {
			result = pathVars.get(((Path)annotation).value());
		} else if(annotation instanceof Header) {
			final String header = ((Header)annotation).value();
			result = ("".equals(header)) ? request.getMethod() :
				request.getHeader(header);
		} else if(annotation instanceof RequestUri) {
			result = requestUri;
		} else if(annotation instanceof Extension) {
			final int dotIndex = requestUri.lastIndexOf(".");
			result = (dotIndex < 0) ? null :
				requestUri.substring(dotIndex+1);
		}
		return result;
	}

}
