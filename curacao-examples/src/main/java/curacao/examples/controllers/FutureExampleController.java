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

package curacao.examples.controllers;

import curacao.annotations.Controller;
import curacao.annotations.RequestMapping;
import curacao.examples.filters.SampleEpochModTwoFilter;

import javax.servlet.AsyncContext;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Controller
public class FutureExampleController {
            
    @RequestMapping(value="^/api/future$", filters=SampleEpochModTwoFilter.class)
    public final Future<String> backToTheFuture(final AsyncContext context) {
        return new BogusRandomPausingFuture();
    }
    
    private final class BogusRandomPausingFuture implements Future<String> {
        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            return false; // NOOP
        }
        @Override
        public boolean isCancelled() {
            return false; // NOOP
        }
        @Override
        public boolean isDone() {
            return false; // NOOP
        }
        @Override
        public String get() throws InterruptedException, ExecutionException {
            // Wait randomly between 1 and 5 seconds.
            final int wait = (new Random().nextInt(4000) + 1000);
            Thread.sleep(wait);
            return String.format("Back to the future!\nWaited %d-milliseconds.", wait);
        }
        @Override
        public String get(final long timeout, final TimeUnit unit)
            throws InterruptedException, ExecutionException, TimeoutException {
            return get();
        }       
    }

}
