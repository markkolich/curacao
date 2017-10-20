/*
 * Copyright (c) 2017 Mark S. Kolich
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

package curacao.mappers.request.types;

import curacao.CuracaoContext;
import curacao.annotations.parameters.*;
import curacao.annotations.parameters.convenience.*;
import curacao.exceptions.requests.MissingRequiredParameterException;
import curacao.mappers.request.ControllerArgumentMapper;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;
import java.lang.annotation.Annotation;

import static com.google.common.net.HttpHeaders.*;

public final class StringMapper extends ControllerArgumentMapper<String> {

	@Override
	public final String resolve(@Nullable final Annotation annotation,
                                @Nonnull final CuracaoContext ctx) throws Exception {
        final HttpServletRequest request = ctx.request_;
        final String requestUri = request.getRequestURI();
		String result = null;
		if (annotation instanceof Accept) {
			result = request.getHeader(ACCEPT);
		} else if (annotation instanceof AcceptCharset) {
			result = request.getHeader(ACCEPT_CHARSET);
		} else if (annotation instanceof AcceptEncoding) {
			result = request.getHeader(ACCEPT_ENCODING);
		} else if (annotation instanceof AcceptLanguage) {
			result = request.getHeader(ACCEPT_LANGUAGE);
		} else if (annotation instanceof Authorization) {
			result = request.getHeader(AUTHORIZATION);
		} else if (annotation instanceof Connection) {
			result = request.getHeader(CONNECTION);
		} else if (annotation instanceof ContentType) {
			result = request.getHeader(CONTENT_TYPE);
		} else if (annotation instanceof Cookie) {
            final String cookieName = ((Cookie)annotation).value();
            result = ("".equals(cookieName)) ?
                // No "value" (name) was provided with this Cookie annotation.
                // Return the raw Cookie request header.
                request.getHeader(COOKIE) :
                // A cookie name was provided, look it up in the incoming Cookie
                // HTTP request header or return null if the cookie by name
                // was not found.
                getCookieByName(request.getCookies(), cookieName);
		} else if (annotation instanceof Date) {
			result = request.getHeader(DATE);
		} else if (annotation instanceof Host) {
			result = request.getHeader(HOST);
		} else if (annotation instanceof IfMatch) {
			result = request.getHeader(IF_MATCH);
		} else if (annotation instanceof IfModifiedSince) {
			result = request.getHeader(IF_MODIFIED_SINCE);
		} else if (annotation instanceof UserAgent) {
			result = request.getHeader(USER_AGENT);
		} else if (annotation instanceof Via) {
			result = request.getHeader(VIA);
		} else if (annotation instanceof Query) {
			final Query query = (Query)annotation;
            result = request.getParameter(query.value());
            if (result == null && query.required()) {
                throw new MissingRequiredParameterException("Request missing required query parameter: " +
                    query.value());
            }
		} else if (annotation instanceof Path) {
            // NOTE: At this point, path variables is guaranteed to be non-null.
            // The invoked controller that got us here is required to return
            // a non-null Map to indicate "yes, I will handle the request".
			result = ctx.getPathVariables().get(((Path) annotation).value());
		} else if (annotation instanceof Header) {
			final String header = ((Header)annotation).value();
			result = ("".equals(header)) ? request.getMethod() : request.getHeader(header);
		} else if (annotation instanceof RequestUri) {
            final boolean includeContext = ((RequestUri)annotation).includeContext();
            result = (includeContext) ?
                // The full request URI, straight from the request.
                requestUri :
                // The URI sans the Servlet context path.  For example, if the
                // request is GET:/foobar/dog/cat and the Servlet context is
                // "foobar" then the path within application would be
                // GET:/dog/cat as extracted.
                ctx.getPathWithinApplication();
		} else if (annotation instanceof Extension) {
			final int dotIndex = requestUri.lastIndexOf(".");
			result = (dotIndex < 0) ? null : requestUri.substring(dotIndex+1);
		}
		return result;
	}

    private static String getCookieByName(final javax.servlet.http.Cookie[] cookies,
										  final String name) {
        String result = null;
        if (cookies != null) {
            for (final javax.servlet.http.Cookie cookie : cookies) {
                if (cookie.getName().equals(name)) {
                    result = cookie.getValue();
                    break;
                }
            }
        }
        return result;
    }

}
