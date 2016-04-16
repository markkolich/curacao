/**
 * Copyright (c) 2016 Mark S. Kolich
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

package curacao.test;

import com.google.common.base.Charsets;
import curacao.test.annotations.CuracaoJUnit4RunnerConfig;
import curacao.test.annotations.MockComponent;
import curacao.test.components.BarComponent;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.asynchttpclient.Response;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletResponse;
import java.util.concurrent.Future;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(CuracaoJUnit4Runner.class)
@CuracaoJUnit4RunnerConfig(port=12000)
public final class CuracaoJUnit4RunnerTest extends AbstractRunnerTest {

    private static final Logger log = LoggerFactory.getLogger(PowermockRunnerTest.class);

    @MockComponent
    private final BarComponent bar_;

    public CuracaoJUnit4RunnerTest() {
        bar_ = new BarComponent(foo_);
    }

    @Test
    public void responseTest() throws Exception {
        try (final AsyncHttpClient asyncHttpClient = new DefaultAsyncHttpClient()) {
            final Future<Response> f = asyncHttpClient.prepareGet("http://localhost:12000").execute();
            final Response r = f.get();
            assertEquals(HttpServletResponse.SC_OK, r.getStatusCode());
            assertEquals("text/plain;charset=utf-8", r.getContentType());
            assertTrue(r.getResponseBody(Charsets.UTF_8).startsWith("Mock for ServletContext"));
        }
    }

}
