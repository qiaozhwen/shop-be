package com.qzshop.shopbe.auth.token;

public class TokenReplayException extends RuntimeException {
    public TokenReplayException(String msg) { super(msg); }
}
