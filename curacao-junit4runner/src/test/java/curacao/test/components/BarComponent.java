package curacao.test.components;

import curacao.annotations.Component;
import curacao.annotations.Injectable;
import curacao.annotations.Required;

@Component
public class BarComponent {

    public final FooComponent foo_;

    @Injectable
    public BarComponent(@Required final FooComponent foo) {
        foo_ = foo;
    }

}
