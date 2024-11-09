/*
 * Copyright (c) 2024 Mark S. Kolich
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

package curacao.util.reflection;

import javax.annotation.Nullable;
import java.lang.annotation.Annotation;

public final class CuracaoAnnotationUtils {

    // Cannot instantiate.
    private CuracaoAnnotationUtils() {
    }

    @Nullable
    public static Annotation getFirstAnnotation(
            final Annotation... list) {
        return getAnnotationSafely(list, 0);
    }

    @Nullable
    public static Annotation getAnnotationSafely(
            final Annotation[] list,
            final int index) {
        return (list.length > 0 && index < list.length) ? list[index] : null;
    }

    public static boolean hasAnnotation(
            final Annotation[] list,
            final Class<? extends Annotation> has) {
        // Quick check; if the annotation array is null or its empty don't even bother walking the for loop below.
        if (list == null || list.length <= 0) {
            return false;
        }
        boolean hasAnnotation = false;
        // Walk each annotation in the list and see if it "compares"; or rather, "is assignable from".
        for (final Annotation annotation : list) {
            if (has.isAssignableFrom(annotation.annotationType())) {
                hasAnnotation = true;
                break;
            }
        }
        return hasAnnotation;
    }

}
