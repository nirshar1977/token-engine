package com.tokenengine.analyser.api;

public interface ContextAnalyser {

    /**
     * Looks for wasted context in a raw Messages API request body. Never throws and never modifies the body;
     * returns {@link WasteReport#none()} when the body can't be analysed.
     */
    WasteReport analyse(byte[] messagesRequestBody);
}
