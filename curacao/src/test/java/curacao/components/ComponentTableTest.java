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

package curacao.components;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import curacao.annotations.Component;
import curacao.annotations.Injectable;
import curacao.core.servlet.ServletContext;
import curacao.exceptions.CuracaoException;
import curacao.exceptions.reflection.AbstractConfigurationException;
import curacao.exceptions.reflection.ArgumentRequiredException;
import curacao.exceptions.reflection.ComponentConfigurationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

public final class ComponentTableTest {

    private static final List<String> EVENTS = Lists.newCopyOnWriteArrayList();

    private final ServletContext context_ = mock(ServletContext.class);

    @BeforeEach
    public void clearEvents() {
        EVENTS.clear();
    }

    @Test
    public void injectsTheOneImplementationOfAnInterfaceTest() {
        final ComponentTable table = table(Circle.class, ShapeConsumer.class);

        final ShapeConsumer consumer = (ShapeConsumer) table.getComponentForType(ShapeConsumer.class);
        assertInstanceOf(Circle.class, consumer.shape_);
        // Every consumer gets the same instance.
        assertSame(table.getComponentForType(Circle.class), consumer.shape_);
        assertSame(consumer.shape_, table.getComponentForType(Shape.class));
    }

    @Test
    public void failsOnAnAmbiguousInjectionTest() {
        final ComponentConfigurationException e = assertThrows(ComponentConfigurationException.class,
                () -> table(Circle.class, Square.class, ShapeConsumer.class));

        assertTrue(e.getMessage().contains(ShapeConsumer.class.getName()), e.getMessage());
        assertTrue(e.getMessage().contains(Circle.class.getName()), e.getMessage());
        assertTrue(e.getMessage().contains(Square.class.getName()), e.getMessage());
        // Two implementations are fine on their own; only an ambiguous injection fails.
        final ComponentTable table = table(Circle.class, Square.class);
        assertInstanceOf(Circle.class, table.getComponentForType(Circle.class));
        assertInstanceOf(Square.class, table.getComponentForType(Square.class));
    }

    @Test
    public void primaryWinsTest() {
        final ComponentTable table = table(Circle.class, PrimaryTriangle.class, ShapeConsumer.class);

        final ShapeConsumer consumer = (ShapeConsumer) table.getComponentForType(ShapeConsumer.class);
        assertInstanceOf(PrimaryTriangle.class, consumer.shape_);
        assertInstanceOf(PrimaryTriangle.class, table.getComponentForType(Shape.class));
    }

    @Test
    public void failsOnTwoPrimariesTest() {
        final ComponentConfigurationException e = assertThrows(ComponentConfigurationException.class,
                () -> table(PrimaryTriangle.class, PrimaryHexagon.class, ShapeConsumer.class));

        assertTrue(e.getMessage().contains("more than one matching component is primary"), e.getMessage());
    }

    @Test
    public void replacedComponentsAreNeverCreatedTest() {
        final ComponentTable table = table(Exploding.class, ReplacesExploding.class, ShapeConsumer.class);

        assertInstanceOf(ReplacesExploding.class, ((ShapeConsumer) table.getComponentForType(ShapeConsumer.class))
                .shape_);
        assertNull(table.getComponentForType(Exploding.class));
    }

    @Test
    public void askingForAReplacedComponentFailsTest() {
        final ArgumentRequiredException e = assertThrows(ArgumentRequiredException.class,
                () -> table(Exploding.class, ReplacesExploding.class, ExplodingConsumer.class));

        assertTrue(e.getMessage().contains("replaced by " + ReplacesExploding.class.getName()), e.getMessage());
    }

    @Test
    public void invalidReplacementsFailTest() {
        assertThrows(ComponentConfigurationException.class, () -> table(ReplacesItself.class));
        // Square isn't in the boot package here.
        assertThrows(ComponentConfigurationException.class, () -> table(ReplacesSquare.class));

        final ComponentConfigurationException cycle = assertThrows(ComponentConfigurationException.class,
                () -> table(CycleA.class, CycleB.class));
        assertTrue(cycle.getMessage().contains("cycle"), cycle.getMessage());
    }

    @Test
    public void replacementsChainTest() {
        // Newer replaces Older, which replaces Exploding: only Newer is left.
        final ComponentTable table = table(Exploding.class, Older.class, Newer.class, ShapeConsumer.class);

        assertInstanceOf(Newer.class, ((ShapeConsumer) table.getComponentForType(ShapeConsumer.class)).shape_);
    }

    @Test
    public void suppressesByPatternTest() {
        final ComponentTable table = table(Exploding.class, SuppressesByPattern.class, ShapeConsumer.class);

        assertInstanceOf(SuppressesByPattern.class, ((ShapeConsumer) table.getComponentForType(
                ShapeConsumer.class)).shape_);
        // A pattern that matches nothing only warns.
        table(SuppressesNothing.class);
    }

    @Test
    public void injectsListsOfEveryImplementationTest() {
        final ComponentTable table = table(Square.class, Circle.class, ShapeList.class, WildcardShapeList.class,
                EmptyList.class);

        final ShapeList shapes = (ShapeList) table.getComponentForType(ShapeList.class);
        // Sorted by class name, whatever order they were found in.
        assertEquals(ImmutableList.of(Circle.class, Square.class), classes(shapes.shapes_));
        assertSame(table.getComponentForType(Circle.class), shapes.shapes_.get(0));

        final WildcardShapeList wildcard = (WildcardShapeList) table.getComponentForType(WildcardShapeList.class);
        assertEquals(ImmutableList.of(Circle.class, Square.class), classes(wildcard.shapes_));

        assertTrue(((EmptyList) table.getComponentForType(EmptyList.class)).colors_.isEmpty());
    }

    @Test
    public void failsOnAMissingDependencyTest() {
        final ArgumentRequiredException e = assertThrows(ArgumentRequiredException.class,
                () -> table(ShapeConsumer.class));

        assertTrue(e.getMessage().contains("no @Component in the boot package is assignable"), e.getMessage());
        assertInstanceOf(AbstractConfigurationException.class, e);
    }

    @Test
    public void failsWithoutAnInjectableOrNoArgumentConstructorTest() {
        // A constructor that takes components but isn't @Injectable used to be called with nulls.
        final ComponentConfigurationException e = assertThrows(ComponentConfigurationException.class,
                () -> table(Circle.class, NotInjectable.class));
        assertTrue(e.getMessage().contains(NotInjectable.class.getName()), e.getMessage());
        assertTrue(e.getMessage().contains("@Injectable"), e.getMessage());
    }

    @Test
    public void failsOnARawListTest() {
        assertThrows(ArgumentRequiredException.class, () -> table(Circle.class, RawList.class));
    }

    @Test
    public void exactClassAndSuperclassesTest() {
        final ComponentTable table = table(Base.class, Derived.class, BaseConsumer.class, AbstractConsumer.class,
                AbstractImpl.class);

        // Asking for a component class by its own type gets that component, even with a subclass around.
        assertInstanceOf(Base.class, ((BaseConsumer) table.getComponentForType(BaseConsumer.class)).base_);
        assertEquals(Base.class, ((BaseConsumer) table.getComponentForType(BaseConsumer.class)).base_.getClass());
        // An abstract superclass is an injection type too.
        assertInstanceOf(AbstractImpl.class, ((AbstractConsumer) table.getComponentForType(AbstractConsumer.class))
                .abstractShape_);
    }

    @Test
    public void failsOnACircularDependencyTest() {
        final ComponentConfigurationException e = assertThrows(ComponentConfigurationException.class,
                () -> table(NeedsChicken.class, NeedsEgg.class));

        assertTrue(e.getMessage().contains("Circular dependency"), e.getMessage());
    }

    @Test
    public void initializesAndDestroysEachComponentOnceInDependencyOrderTest() {
        final ComponentTable table = table(Car.class, Engine.class);

        table.initializeAll();
        table.initializeAll();
        table.destroyAll();
        table.destroyAll();

        // Engine is a dependency of Car: initialized first, destroyed last, and each exactly once, even though
        // Engine is also reachable through its Part interface.
        assertEquals(ImmutableList.of("init Engine", "init Car", "destroy Car", "destroy Engine"), EVENTS);
    }

    @Test
    public void failedInitializeDestroysTheComponentsAlreadyInitializedTest() {
        final ComponentTable table = table(Engine.class, FailsToInitialize.class);
        EVENTS.clear();

        final CuracaoException e = assertThrows(CuracaoException.class, table::initializeAll);
        assertTrue(e.getMessage().contains(FailsToInitialize.class.getCanonicalName()), e.getMessage());
        assertInstanceOf(IllegalStateException.class, e.getCause());

        // Engine was initialized, so it's destroyed; the component that failed never finished initializing, so
        // it isn't. A destroyAll() after the failure has nothing left to do.
        table.destroyAll();
        assertEquals(ImmutableList.of("init Engine", "destroy Engine"), EVENTS);
    }

    @Test
    public void destroyAllCarriesOnPastAFailureTest() {
        final ComponentTable table = table(Engine.class, FailsToDestroy.class).initializeAll();
        EVENTS.clear();

        final CuracaoException e = assertThrows(CuracaoException.class, table::destroyAll);
        assertTrue(e.getMessage().contains(FailsToDestroy.class.getCanonicalName()), e.getMessage());
        assertInstanceOf(IllegalStateException.class, e.getCause());

        // FailsToDestroy depends on Engine, so it's destroyed first; its failure doesn't stop Engine's destroy.
        assertEquals(ImmutableList.of("destroy Engine"), EVENTS);
    }

    @Test
    public void resolvesArgumentsForControllersTest() throws Exception {
        final ComponentTable table = table(Circle.class, Square.class);

        final Object[] arguments = table.resolveArguments(
                ShapeController.class.getConstructor(List.class, ServletContext.class), ShapeController.class);
        assertEquals(ImmutableList.of(Circle.class, Square.class), classes((List<?>) arguments[0]));
        assertSame(context_, arguments[1]);

        assertThrows(ComponentConfigurationException.class, () -> table.resolveArguments(
                ShapeConsumer.class.getConstructor(Shape.class), ShapeConsumer.class));
    }

    @Test
    public void getComponentForTypeTest() {
        final ComponentTable table = table(Circle.class, Square.class);

        assertSame(context_, table.getComponentForType(ServletContext.class));
        assertNull(table.getComponentForType(String.class));
        assertInstanceOf(Circle.class, table.getComponentForType(Circle.class));
        assertThrows(ComponentConfigurationException.class, () -> table.getComponentForType(Shape.class));
    }

    private ComponentTable table(
            final Class<?>... components) {
        final Set<Class<?>> scanned = ImmutableSet.copyOf(components);

        return new ComponentTable(context_, scanned);
    }

    private static List<Class<?>> classes(
            final List<?> instances) {
        final List<Class<?>> classes = Lists.newArrayList();
        instances.forEach(instance -> classes.add(instance.getClass()));

        return classes;
    }

    // Fixtures.

    public interface Shape {
    }

    public interface Color {
    }

    public interface Part {
    }

    @Component
    public static final class Circle implements Shape {
    }

    @Component
    public static final class Square implements Shape {
    }

    @Component(primary = true)
    public static final class PrimaryTriangle implements Shape {
    }

    @Component(primary = true)
    public static final class PrimaryHexagon implements Shape {
    }

    @Component
    public static final class Exploding implements Shape {
        public Exploding() {
            throw new IllegalStateException("A replaced component must never be created.");
        }
    }

    @Component(replaces = Exploding.class)
    public static final class ReplacesExploding implements Shape {
    }

    @Component(replaces = Exploding.class)
    public static final class Older implements Shape {
    }

    @Component(replaces = Older.class)
    public static final class Newer implements Shape {
    }

    @Component(suppress = "curacao.components.ComponentTableTest.Explod*")
    public static final class SuppressesByPattern implements Shape {
    }

    @Component(suppress = "com.example.missing.*")
    public static final class SuppressesNothing {
    }

    @Component(replaces = ReplacesItself.class)
    public static final class ReplacesItself {
    }

    @Component(replaces = Square.class)
    public static final class ReplacesSquare {
    }

    @Component(replaces = CycleB.class)
    public static final class CycleA {
    }

    @Component(replaces = CycleA.class)
    public static final class CycleB {
    }

    @Component
    public static final class ShapeConsumer {
        private final Shape shape_;

        @Injectable
        public ShapeConsumer(
                final Shape shape) {
            shape_ = shape;
        }
    }

    @Component
    public static final class ExplodingConsumer {
        @Injectable
        public ExplodingConsumer(
                final Exploding exploding) {
            EVENTS.add("created with " + exploding);
        }
    }

    @Component
    public static final class ShapeList {
        private final List<Shape> shapes_;

        @Injectable
        public ShapeList(
                final List<Shape> shapes) {
            shapes_ = shapes;
        }
    }

    @Component
    public static final class WildcardShapeList {
        private final List<? extends Shape> shapes_;

        @Injectable
        public WildcardShapeList(
                final List<? extends Shape> shapes) {
            shapes_ = shapes;
        }
    }

    @Component
    public static final class EmptyList {
        private final List<Color> colors_;

        @Injectable
        public EmptyList(
                final List<Color> colors) {
            colors_ = colors;
        }
    }

    @Component
    @SuppressWarnings("rawtypes")
    public static final class RawList {
        @Injectable
        public RawList(
                final List shapes) {
            EVENTS.add("created with " + shapes);
        }
    }

    /**
     * Not final, for its subclass: package-private, since only public classes must be final.
     */
    @Component
    /* package */ static class Base {
        public Base() {
        }
    }

    @Component
    public static final class Derived extends Base {
    }

    @Component
    public static final class BaseConsumer {
        private final Base base_;

        @Injectable
        public BaseConsumer(
                final Base base) {
            base_ = base;
        }
    }

    public abstract static class AbstractShape implements Shape {
    }

    @Component
    public static final class AbstractImpl extends AbstractShape {
    }

    @Component
    public static final class AbstractConsumer {
        private final AbstractShape abstractShape_;

        @Injectable
        public AbstractConsumer(
                final AbstractShape abstractShape) {
            abstractShape_ = abstractShape;
        }
    }

    @Component
    public static final class NeedsChicken {
        @Injectable
        public NeedsChicken(
                final NeedsEgg egg) {
            EVENTS.add("created with " + egg);
        }
    }

    @Component
    public static final class NeedsEgg {
        @Injectable
        public NeedsEgg(
                final NeedsChicken chicken) {
            EVENTS.add("created with " + chicken);
        }
    }

    @Component
    public static final class Engine implements Part, CuracaoComponent {
        @Override
        public void initialize() {
            EVENTS.add("init Engine");
        }

        @Override
        public void destroy() {
            EVENTS.add("destroy Engine");
        }
    }

    @Component
    public static final class Car implements CuracaoComponent {
        @Injectable
        public Car(
                final Part engine) {
            EVENTS.add("created with " + engine.getClass().getSimpleName());
            EVENTS.clear();
        }

        @Override
        public void initialize() {
            EVENTS.add("init Car");
        }

        @Override
        public void destroy() {
            EVENTS.add("destroy Car");
        }
    }

    @Component
    public static final class FailsToInitialize implements CuracaoComponent {
        @Injectable
        public FailsToInitialize(
                final Part engine) {
            EVENTS.add("created with " + engine.getClass().getSimpleName());
        }

        @Override
        public void initialize() {
            throw new IllegalStateException("Can't start.");
        }

        @Override
        public void destroy() {
            EVENTS.add("destroy FailsToInitialize");
        }
    }

    @Component
    public static final class FailsToDestroy implements CuracaoComponent {
        @Injectable
        public FailsToDestroy(
                final Part engine) {
            EVENTS.add("created with " + engine.getClass().getSimpleName());
        }

        @Override
        public void initialize() {
            EVENTS.add("init FailsToDestroy");
        }

        @Override
        public void destroy() {
            throw new IllegalStateException("Can't stop.");
        }
    }

    @Component
    public static final class NotInjectable {
        public NotInjectable(
                final Shape shape) {
            EVENTS.add("created with " + shape);
        }
    }

    public static final class ShapeController {
        @Injectable
        public ShapeController(
                final List<Shape> shapes,
                final ServletContext context) {
            EVENTS.add("created with " + shapes + " " + context);
        }
    }

}
