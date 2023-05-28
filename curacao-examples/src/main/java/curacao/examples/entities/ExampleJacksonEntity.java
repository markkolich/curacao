/*
 * Copyright (c) 2023 Mark S. Kolich
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

package curacao.examples.entities;

import curacao.jackson.AbstractJacksonAppendableCuracaoEntity;

public final class ExampleJacksonEntity extends AbstractJacksonAppendableCuracaoEntity {

    private String foo_;
    private long bar_;

    public ExampleJacksonEntity(
            final String foo,
            final long bar) {
        foo_ = foo;
        bar_ = bar;
    }

    // Required for Jackson
    public ExampleJacksonEntity() {
        this(null, -1L);
    }

    public String getFoo() {
        return foo_;
    }

    public void setFoo(
            final String foo) {
        foo_ = foo;
    }

    public long getBar() {
        return bar_;
    }

    public void setBar(
            final long bar) {
        bar_ = bar;
    }

    @Override
    public String toString() {
        return String.format("ExampleJacksonEntity(%s,%d)", foo_, bar_);
    }

}
