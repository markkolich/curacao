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

package curacao.examples.mappers;

import com.fasterxml.jackson.databind.ObjectMapper;
import curacao.annotations.Injectable;
import curacao.annotations.Mapper;
import curacao.examples.components.JacksonComponent;
import curacao.examples.entities.ExampleJacksonEntity;
import curacao.mappers.request.types.body.InputStreamReaderRequestBodyMapper;

import java.io.InputStreamReader;

@Mapper
public final class ExampleJacksonArgumentMapper extends InputStreamReaderRequestBodyMapper<ExampleJacksonEntity> {

    private final ObjectMapper mapper_;

    @Injectable
    public ExampleJacksonArgumentMapper(final JacksonComponent jackson) {
        mapper_ = jackson.getMapperInstance();
    }

    @Override
    public final ExampleJacksonEntity resolveWithReader(final InputStreamReader reader) throws Exception {
        return mapper_.readValue(reader, ExampleJacksonEntity.class);
    }

}
