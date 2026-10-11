/*
 * Copyright (c) 2026 Mark S. Kolich
 * https://mark.koli.ch
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

package curacao.freemarker.mappers;

import curacao.core.servlet.HttpResponse;
import curacao.entities.mediatype.document.TextPlainUtf8CuracaoEntity;
import curacao.freemarker.components.AbstractFreeMarkerContentRenderer;
import curacao.freemarker.entities.FreeMarkerContent;
import curacao.freemarker.entities.Utf8TextEntity;
import curacao.mappers.response.AbstractControllerReturnTypeMapper;
import org.slf4j.Logger;

import javax.annotation.Nonnull;

import static com.google.common.base.Preconditions.checkNotNull;
import static curacao.core.servlet.HttpStatus.SC_INTERNAL_SERVER_ERROR;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * A response mapper that renders {@link FreeMarkerContent}: either the content a controller
 * returns ({@code T} is {@link FreeMarkerContent}), or a page for some other type, e.g. an
 * exception mapper that renders an error page template.
 *
 * Curacao only discovers mappers in the application's own boot package, so applications extend
 * this with a {@link curacao.annotations.Mapper} of their own, whose
 * {@link curacao.annotations.Injectable} constructor passes in their renderer, and whose
 * {@link #render} calls {@link #renderFreeMarkerContent}. Rendered bodies are passed through the
 * content's own {@link FreeMarkerContent#getTransformers() transformers}, if it has any.
 *
 * If the template fails to render, the failure is logged and a plain-text 500 is sent instead.
 * Without this, Curacao would log the exception and complete the request with nothing written: an
 * empty 200.
 *
 * @param <T> the type this mapper renders
 */
public abstract class AbstractFreeMarkerContentResponseMapper<T>
        extends AbstractControllerReturnTypeMapper<T> {

    private static final Logger LOG = getLogger(AbstractFreeMarkerContentResponseMapper.class);

    private static final String RENDER_FAILURE_BODY = "500 Internal Server Error";

    protected final AbstractFreeMarkerContentRenderer fmcRenderer_;

    protected AbstractFreeMarkerContentResponseMapper(
            @Nonnull final AbstractFreeMarkerContentRenderer fmcRenderer) {
        fmcRenderer_ = checkNotNull(fmcRenderer, "FreeMarker content renderer cannot be null.");
    }

    protected final void renderFreeMarkerContent(
            final HttpResponse response,
            final FreeMarkerContent content) throws Exception {
        try {
            final String rendered = fmcRenderer_.contentToString(content);
            renderEntity(response, new Utf8TextEntity.Builder(content.getMediaType())
                    .setStatus(content.getStatus())
                    .setBody(rendered)
                    .addTransformers(content.getTransformers())
                    .build());
        } catch (final Exception e) {
            // Nothing has been written to the response yet, so it can still become a 500.
            LOG.error("Failed to render FreeMarker template: {}", content.getTemplateName(), e);
            renderEntity(response, new TextPlainUtf8CuracaoEntity(SC_INTERNAL_SERVER_ERROR, RENDER_FAILURE_BODY));
        }
    }

}
