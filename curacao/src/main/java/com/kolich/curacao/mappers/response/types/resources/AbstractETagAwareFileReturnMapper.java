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

package com.kolich.curacao.mappers.response.types.resources;

import com.kolich.curacao.entities.CuracaoEntity;
import com.kolich.curacao.mappers.response.ControllerReturnTypeMapper;
import org.apache.commons.codec.digest.DigestUtils;

import javax.annotation.Nonnull;
import javax.servlet.AsyncContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;

import static com.google.common.net.HttpHeaders.IF_NONE_MATCH;

/* intentionally abstract to be extended as needed */
public abstract class AbstractETagAwareFileReturnMapper extends ControllerReturnTypeMapper<File> {

    private static final String WEAK_ETAG_HEADER_FORMAT = "W/\"%s\"";

    @Override
    public void render(final AsyncContext context,
                       final HttpServletResponse response,
                       @Nonnull final File entity) throws Exception {
        final String ifNoneMatch = getIfNoneMatchFromRequest(context);
        final String eTag = getETag(entity); // Always non-null
        // If the incoming "If-None-Match" HTTP request header matches the
        // current ETag for the requested resource, then the content must be
        // the same and we send back a 304 Not Modified response.  Note the
        // "*" wildcard which indicates if there are any ~any~ matches.
        final CuracaoEntity result;
        if (eTag.equals(ifNoneMatch) || "*".equals(ifNoneMatch)) {
            result = new NotModifiedResponseEntity(response, entity, eTag);
        } else {
            result = new FileResponseEntity(response, entity, eTag);
        }
        renderEntity(response, result);
    }

    private static final String getETag(final File f) {
        final String hash = getSHA1Hash(
            // The name of the file.
            f.getName() +
            // The size of the file on disk, in bytes.
            f.length() +
            // The timestamp of when the file was last modified.
            f.lastModified()
        );
        return String.format(WEAK_ETAG_HEADER_FORMAT, hash);
    }

    private static final String getIfNoneMatchFromRequest(final AsyncContext context) {
        final HttpServletRequest request = (HttpServletRequest)context.getRequest();
        return request.getHeader(IF_NONE_MATCH);
    }

    private static final String getSHA1Hash(final String input) {
        return DigestUtils.shaHex(input);
    }

}
