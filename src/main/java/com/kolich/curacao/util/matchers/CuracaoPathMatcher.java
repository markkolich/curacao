package com.kolich.curacao.util.matchers;

import java.util.Map;

public interface CuracaoPathMatcher {
	
	public boolean matches(final String pattern, final String path);
	
	public Map<String,String> extractUriTemplateVariables(
		final String pattern, final String path);

}
