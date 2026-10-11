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

package curacao.freemarker.components;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import curacao.freemarker.entities.FreeMarkerContent;
import curacao.freemarker.exceptions.CuracaoFreeMarkerException;
import freemarker.template.Configuration;
import freemarker.template.Template;

import javax.annotation.Nonnull;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Renders {@link FreeMarkerContent} with a FreeMarker {@link Configuration}.
 *
 * The {@link Configuration} is entirely the application's: it builds one (template loader,
 * encoding, lookup strategy, incompatible improvements version, and so on) and passes it to this
 * constructor. Curacao doesn't configure FreeMarker itself.
 *
 * A template's data is merged from three sources, later ones winning: the
 * {@link #getGlobalDataMap() global data map} every template gets (applications override it to add
 * e.g. a context path), the content's own {@link FreeMarkerContent#getDataMap() data map}, and the
 * custom attributes declared in the template itself ({@code <#ftl attributes={...}>}).
 *
 * Curacao only discovers components in the application's own boot package, so this isn't a
 * {@link curacao.annotations.Component} itself: applications extend it with a
 * {@link curacao.annotations.Component} of their own, whose {@link curacao.annotations.Injectable}
 * constructor passes in their {@link Configuration}.
 */
public abstract class AbstractFreeMarkerContentRenderer {

    private final Configuration freemarkerConfig_;

    protected AbstractFreeMarkerContentRenderer(
            @Nonnull final Configuration freemarkerConfig) {
        freemarkerConfig_ = checkNotNull(freemarkerConfig, "FreeMarker configuration cannot be null.");
    }

    public final String contentToString(
            final FreeMarkerContent content) throws Exception {
        try (Writer w = contentToWriter(content)) {
            return w.toString();
        }
    }

    public final Writer contentToWriter(
            final FreeMarkerContent content) throws Exception {
        final Writer w = new StringWriter();
        try {
            final Template tp = freemarkerConfig_.getTemplate(content.getTemplateName());
            tp.process(getMergedTemplateDataMap(tp, content), w);
        } catch (final Exception e) {
            throw new CuracaoFreeMarkerException("Failed to process free marker template into string.", e);
        }
        return w;
    }

    /**
     * The data every template gets, under the content's own data and the template's attributes.
     * Empty by default; applications override this to add their own shared values.
     *
     * @return the global template data
     */
    @Nonnull
    public Map<String, Object> getGlobalDataMap() {
        return ImmutableMap.of();
    }

    private Map<String, Object> getMergedTemplateDataMap(
            final Template tp,
            final FreeMarkerContent content) {
        // The resulting map is intentionally not immutable here because it's
        // mutated during template processing.
        final Map<String, Object> map = Maps.newLinkedHashMap();
        // Merge in the web-application specific data map.
        map.putAll(getGlobalDataMap());
        // Get the entity defined set of template attributes.
        map.putAll(content.getDataMap());
        // Get the template defined set of attributes (these are defined
        // inline within the .ftl template file itself).
        for (final String attrName : tp.getCustomAttributeNames()) {
            map.put(attrName, tp.getCustomAttribute(attrName));
        }
        return ImmutableMap.copyOf(map);
    }

}
