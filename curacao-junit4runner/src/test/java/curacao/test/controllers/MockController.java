package curacao.test.controllers;

import curacao.annotations.Controller;
import curacao.annotations.Injectable;
import curacao.annotations.RequestMapping;
import curacao.annotations.Required;
import curacao.test.components.BarComponent;

@Controller
public final class MockController {

    private final BarComponent bar_;

    @Injectable
    public MockController(@Required final BarComponent bar) {
        bar_ = bar;
    }

    @RequestMapping("\\/$")
    public String helloWorld() {
        return bar_.foo_.context_.toString();
    }

}
