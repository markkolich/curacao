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

package curacao;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;
import com.typesafe.config.ConfigFactory;
import curacao.exceptions.CuracaoException;
import org.slf4j.Logger;

import java.util.concurrent.TimeUnit;

import static org.slf4j.LoggerFactory.getLogger;

public final class CuracaoConfigLoader {

	private static final Logger log = getLogger(CuracaoConfigLoader.class);

	private static final String CURACAO_CONFIG = "curacao";

	private static final String BOOT_PACKAGE = "boot-package";

	private static final String ASYNC_CONTEXT_TIMEOUT = "async-context-timeout";

    private static final String THREAD_POOL = "thread-pool";

	private static final String SIZE = "size";
	private static final String NAME_FORMAT = "name-format";

	private static final String MAPPERS_REQUEST = "mappers.request";
	private static final String MAPPERS_RESPONSE = "mappers.response";

	private static final String DEFAULT_MAX_REQUEST_BODY_SIZE = "max-request-body-size";
	private static final String DEFAULT_CHAR_ENCODING_IF_NOT_SPECIFIED = "default-character-encoding-if-not-specified";

    private static final String CONTENT_TYPES = "content-types";

    private final Config config_;

	private CuracaoConfigLoader() {
		try {
			config_ = ConfigFactory.load();
			log.debug("Loaded configuration: {}", config_.toString());
		} catch (ConfigException ce) {
			throw new CuracaoException("Failed to find valid configuration.", ce);
		}
	}
	private static final class LazyHolder {
		private static final CuracaoConfigLoader instance = new CuracaoConfigLoader();
	}

	public static final Config getConfig() {
		return LazyHolder.instance.config_;
	}

	public static final String getBaseConfigPath(final String path) {
		return String.format("%s.%s", CURACAO_CONFIG, path);
	}

	public static final String getConfigStringProperty(final String property) {
		return getConfig().getString(getBaseConfigPath(property));
	}

	public static final Boolean getConfigBooleanProperty(final String property) {
		return getConfig().getBoolean(getBaseConfigPath(property));
	}

	public static final Long getConfigLongProperty(final String property) {
		return getConfig().getLong(getBaseConfigPath(property));
	}

	public static final Integer getConfigIntProperty(final String property) {
		return getConfig().getInt(getBaseConfigPath(property));
	}

	public static final Long getMillisecondsConfigProperty(final String property) {
		return getConfig().getDuration(getBaseConfigPath(property), TimeUnit.MILLISECONDS);
	}

	public static final Long getBytesConfigProperty(final String property) {
		return getConfig().getBytes(getBaseConfigPath(property));
	}

	// Config specific helper methods.

	public static final String getThreadPoolConfigPropertyPath(final String property) {
		return String.format("%s.%s", THREAD_POOL, property);
	}

	public static final String getRequestMappersConfigProperty(final String property) {
		return String.format("%s.%s", MAPPERS_REQUEST, property);
	}
	public static final String getResponseMappersConfigProperty(final String property) {
		return String.format("%s.%s", MAPPERS_RESPONSE, property);
	}

	// Property specific helper methods.

	public static final String getBootPackage() {
		return getConfigStringProperty(BOOT_PACKAGE);
	}

	public static final Long getAsyncContextTimeoutMs() {
		return getMillisecondsConfigProperty(ASYNC_CONTEXT_TIMEOUT);
	}

	// Thread pool configurations.

	public static final Integer getThreadPoolSize() {
		return getConfigIntProperty(getThreadPoolConfigPropertyPath(SIZE));
	}

	public static final String getThreadPoolNameFormat() {
		return getConfigStringProperty(getThreadPoolConfigPropertyPath(NAME_FORMAT));
	}

	// Request mapper configurations.

	public static final Long getDefaultMaxRequestBodySizeInBytes() {
		return getBytesConfigProperty(getRequestMappersConfigProperty(DEFAULT_MAX_REQUEST_BODY_SIZE));
	}

	public static final String getDefaultCharEncodingIfNotSpecified() {
		return getConfigStringProperty(getRequestMappersConfigProperty(DEFAULT_CHAR_ENCODING_IF_NOT_SPECIFIED));
	}

    // Content type helpers.

    public static final String getContentTypeForExtension(final String ext,
														  final String defaultValue) {
        String contentType = defaultValue;
        try {
            contentType = getConfig().getConfig(getBaseConfigPath(CONTENT_TYPES)).getString(ext);
        } catch (Exception e) { }
        return contentType;
    }

}
