package curacao.embedded.filters;

import org.slf4j.Logger;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static org.slf4j.LoggerFactory.getLogger;

public final class FooFilter implements Filter {

    private static final Logger log = getLogger(FooFilter.class);

    @Override
    public void init(FilterConfig filterConfig) throws ServletException { }

    @Override
    public void destroy() { }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest sRequest = (HttpServletRequest) request;
        HttpServletResponse sResponse = (HttpServletResponse) response;
        long now = System.currentTimeMillis();
        try {
            //chain.doFilter(request, wrapper);
            chain.doFilter(request, sResponse);
        } finally {
            sResponse.addHeader("X-Timing", Long.toString(System.currentTimeMillis()-now) + "-ms");
        }
    }

}
