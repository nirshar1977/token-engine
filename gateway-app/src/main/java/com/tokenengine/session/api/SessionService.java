package com.tokenengine.session.api;

import com.tokenengine.shared.api.DeveloperId;

public interface SessionService {

    /**
     * Records one more request for the session and refreshes its sliding TTL.
     * Never throws: if the store is down it returns a degraded context so traffic keeps flowing.
     *
     * @param sessionHint the client's session id, or null when the client sent none
     */
    SessionContext touch(DeveloperId developer, String sessionHint);
}
