package com.tokenengine.gateway.internal;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

/** Where the proxied response goes. Decouples the proxy core from the servlet API so it can be tested alone. */
interface ResponseSink {

    void start(int status, Map<String, List<String>> headers) throws IOException;

    OutputStream body() throws IOException;
}
