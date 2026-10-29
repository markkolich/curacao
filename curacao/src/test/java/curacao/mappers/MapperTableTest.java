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

package curacao.mappers;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import curacao.components.ComponentTable;
import curacao.context.CuracaoContext;
import curacao.core.servlet.AsyncContext;
import curacao.core.servlet.HttpResponse;
import curacao.entities.CuracaoEntity;
import curacao.exceptions.reflection.ComponentConfigurationException;
import curacao.exceptions.routing.PathNotFoundException;
import curacao.mappers.request.AbstractControllerArgumentMapper;
import curacao.mappers.request.types.StringMapper;
import curacao.mappers.response.AbstractControllerReturnTypeMapper;
import curacao.mappers.response.types.CuracaoEntityReturnMapper;
import curacao.mappers.response.types.DefaultObjectReturnMapper;
import org.junit.jupiter.api.Test;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.annotation.Annotation;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

public final class MapperTableTest {

    @Test
    public void theMostSpecificMapperWinsTest() {
        final MapperTable table = table(ExceptionMapper.class, ParentMapper.class, ChildMapper.class);

        assertInstanceOf(ChildMapper.class, table.getReturnTypeMapperForClass(ChildException.class));
        assertInstanceOf(ParentMapper.class, table.getReturnTypeMapperForClass(ParentException.class));
        assertInstanceOf(ExceptionMapper.class, table.getReturnTypeMapperForClass(IllegalStateException.class));
        // Twice, from the cache.
        assertInstanceOf(ChildMapper.class, table.getReturnTypeMapperForClass(ChildException.class));
    }

    @Test
    public void applicationMappersBeatBuiltInOnesTest() {
        final MapperTable table = table(ExceptionMapper.class);

        // Curacao has a more specific built-in mapper for its routing exceptions (CuracaoException.WithEntity),
        // but the application's mapper for Exception still renders them.
        assertInstanceOf(ExceptionMapper.class, table.getReturnTypeMapperForClass(PathNotFoundException.class));
        // Types no application mapper renders go to the built-in ones.
        assertInstanceOf(CuracaoEntityReturnMapper.class, table.getReturnTypeMapperForClass(CuracaoEntity.class));
        assertInstanceOf(DefaultObjectReturnMapper.class, table.getReturnTypeMapperForClass(String.class));
    }

    @Test
    public void twoMappersForOneTypeFailTest() {
        final ComponentConfigurationException e = assertThrows(ComponentConfigurationException.class,
                () -> table(ParentMapper.class, OtherParentMapper.class));

        assertTrue(e.getMessage().contains(ParentMapper.class.getName()), e.getMessage());
        assertTrue(e.getMessage().contains(OtherParentMapper.class.getName()), e.getMessage());
    }

    @Test
    public void argumentMappersRunInNameOrderBeforeBuiltInOnesTest() {
        // Given in reverse order, to show the table sorts them.
        final MapperTable table = table(ZebraStringMapper.class, AlphaStringMapper.class);

        final List<Class<?>> order = table.getArgumentMappersForClass(String.class).stream()
                .map(Object::getClass)
                .collect(ImmutableList.toImmutableList());
        // The application's mappers first, by name, then Curacao's built-in ones.
        assertEquals(ImmutableList.of(AlphaStringMapper.class, ZebraStringMapper.class), order.subList(0, 2));
        assertTrue(order.contains(StringMapper.class), order.toString());
    }

    @Test
    public void mappersNeedAnInjectableOrNoArgumentConstructorTest() {
        final ComponentConfigurationException e = assertThrows(ComponentConfigurationException.class,
                () -> table(NotInjectableMapper.class));

        assertTrue(e.getMessage().contains(NotInjectableMapper.class.getName()), e.getMessage());
    }

    private static MapperTable table(
            final Class<?>... mappers) {
        return new MapperTable(mock(ComponentTable.class), ImmutableSet.copyOf(mappers));
    }

    // Fixtures.

    public static class ParentException extends Exception {
    }

    public static final class ChildException extends ParentException {
    }

    public abstract static class AbstractNoopMapper<T> extends AbstractControllerReturnTypeMapper<T> {
        @Override
        public void render(
                final AsyncContext context,
                final HttpResponse response,
                @Nonnull final T entity) {
            // Never rendered here.
        }
    }

    public static final class ExceptionMapper extends AbstractNoopMapper<Exception> {
    }

    public static final class ParentMapper extends AbstractNoopMapper<ParentException> {
    }

    public static final class OtherParentMapper extends AbstractNoopMapper<ParentException> {
    }

    public static final class ChildMapper extends AbstractNoopMapper<ChildException> {
    }

    public static final class NotInjectableMapper extends AbstractNoopMapper<String> {
        public NotInjectableMapper(
                final Object dependency) {
            // A constructor that takes something, without @Injectable.
        }
    }

    public abstract static class AbstractNullStringMapper extends AbstractControllerArgumentMapper<String> {
        @Override
        public String resolve(
                @Nullable final Annotation annotation,
                @Nonnull final CuracaoContext context) {
            return null;
        }
    }

    public static final class AlphaStringMapper extends AbstractNullStringMapper {
    }

    public static final class ZebraStringMapper extends AbstractNullStringMapper {
    }

}
