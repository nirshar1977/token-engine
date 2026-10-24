package com.tokenengine.gateway.internal;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

final class ServletResponseSink implements ResponseSink {

    private final HttpServletResponse response;

    ServletResponseSink(HttpServletResponse response) {
        this.response = response;
    }

    @Override
    public void start(int status, Map<String, List<String>> headers) {
        response.setStatus(status);
        headers.forEach((name, values) -> values.forEach(value -> response.addHeader(name, value)));
    }

    @Override
    public OutputStream body() throws IOException {
        return response.getOutputStream();
    }
}
