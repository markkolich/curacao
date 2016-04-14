package curacao.test;

import curacao.test.annotations.MockComponent;
import curacao.test.components.FooComponent;
import org.mockito.Mockito;

import javax.servlet.ServletContext;

public abstract class AbstractRunnerTest {

    @MockComponent
    protected final FooComponent foo_;

    public AbstractRunnerTest() {
        final ServletContext mockContext = Mockito.mock(ServletContext.class);

        foo_ = new FooComponent(mockContext);
    }

}
