package org.kohsuke.github;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.InjectableValues;
import com.fasterxml.jackson.databind.JsonMappingException;
import org.apache.commons.io.IOUtils;
import org.kohsuke.github.connector.GitHubConnectorResponse;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

/**
 * A GitHubResponse
 * <p>
 * A {@link GitHubResponse} generated by sending a {@link GitHubRequest} to a {@link GitHubClient}.
 * </p>
 *
 * @param <T>
 *            the type of the data parsed from the body of a {@link GitHubConnectorResponse}.
 *
 * @author Liam Newman
 */
class GitHubResponse<T> {

    private static final Logger LOGGER = Logger.getLogger(GitHubResponse.class.getName());

    private final int statusCode;

    @Nonnull
    private final Map<String, List<String>> headers;

    @CheckForNull
    private final T body;

    GitHubResponse(GitHubResponse<T> response, @CheckForNull T body) {
        this.statusCode = response.statusCode();
        this.headers = response.headers;
        this.body = body;
    }

    GitHubResponse(GitHubConnectorResponse connectorResponse, @CheckForNull T body) {
        this.statusCode = connectorResponse.statusCode();
        this.headers = connectorResponse.allHeaders();
        this.body = body;
    }

    /**
     * Parses a {@link GitHubConnectorResponse} body into a new instance of {@link T}.
     *
     * @param connectorResponse
     *            response info to parse.
     * @param type
     *            the type to be constructed.
     * @param <T>
     *            the type
     * @return a new instance of {@link T}.
     * @throws IOException
     *             if there is an I/O Exception.
     */
    @CheckForNull
    static <T> T parseBody(GitHubConnectorResponse connectorResponse, Class<T> type) throws IOException {

        if (connectorResponse.statusCode() == HttpURLConnection.HTTP_NO_CONTENT) {
            if (type != null && type.isArray()) {
                // no content for array should be empty array
                return type.cast(Array.newInstance(type.getComponentType(), 0));
            } else {
                // no content for object should be null
                return null;
            }
        }

        String data = getBodyAsString(connectorResponse);
        try {
            InjectableValues.Std inject = new InjectableValues.Std();
            inject.addValue(GitHubConnectorResponse.class, connectorResponse);

            return GitHubClient.getMappingObjectReader(connectorResponse).forType(type).readValue(data);
        } catch (JsonMappingException | JsonParseException e) {
            String message = "Failed to deserialize: " + data;
            LOGGER.log(Level.FINE, message);
            throw e;
        }
    }

    /**
     * Parses a {@link GitHubConnectorResponse} body into a new instance of {@link T}.
     *
     * @param connectorResponse
     *            response info to parse.
     * @param instance
     *            the object to fill with data parsed from body
     * @param <T>
     *            the type
     * @return a new instance of {@link T}.
     * @throws IOException
     *             if there is an I/O Exception.
     */
    @CheckForNull
    static <T> T parseBody(GitHubConnectorResponse connectorResponse, T instance) throws IOException {

        String data = getBodyAsString(connectorResponse);
        try {
            return GitHubClient.getMappingObjectReader(connectorResponse).withValueToUpdate(instance).readValue(data);
        } catch (JsonMappingException | JsonParseException e) {
            String message = "Failed to deserialize: " + data;
            LOGGER.log(Level.FINE, message);
            throw e;
        }
    }

    /**
     * Gets the body of the response as a {@link String}.
     *
     * @return the body of the response as a {@link String}.
     * @throws IOException
     *             if an I/O Exception occurs.
     * @param connectorResponse
     */
    @Nonnull
    static String getBodyAsString(GitHubConnectorResponse connectorResponse) throws IOException {
        InputStream inputStream = connectorResponse.bodyStream();
        try (InputStreamReader r = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
            return IOUtils.toString(r);
        }
    }

    /**
     * Gets the body of the response as a {@link String}.
     *
     * @return the body of the response as a {@link String}.
     * @throws IOException
     *             if an I/O Exception occurs.
     * @param connectorResponse
     */
    static String getBodyAsStringOrNull(GitHubConnectorResponse connectorResponse) {
        try {
            return getBodyAsString(connectorResponse);
        } catch (NullPointerException | IOException e) {
        }
        return null;
    }

    /**
     * The status code for this response.
     *
     * @return the status code for this response.
     */
    public int statusCode() {
        return statusCode;
    }

    /**
     * The headers for this response.
     *
     * @return the headers for this response.
     */
    @Nonnull
    public List<String> headers(String field) {
        return headers.get(field);
    }

    /**
     * Gets the value of a header field for this response.
     *
     * @param name
     *            the name of the header field.
     * @return the value of the header field, or {@code null} if the header isn't set.
     */
    @CheckForNull
    public String header(String name) {
        String result = null;
        List<String> rawResult = headers.get(name);
        if (rawResult != null) {
            result = rawResult.get(0);
        }
        return result;
    }

    /**
     * The body of the response parsed as a {@link T}
     *
     * @return body of the response
     */
    public T body() {
        return body;
    }

}
