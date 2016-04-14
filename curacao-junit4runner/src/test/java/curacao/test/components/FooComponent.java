package curacao.test.components;

import curacao.annotations.Component;
import curacao.annotations.Injectable;
import curacao.annotations.Required;

import javax.servlet.ServletContext;

@Component
public class FooComponent {

    public final ServletContext context_;

    @Injectable
    public FooComponent(@Required final ServletContext context) {
        context_ = context;
    }

}
