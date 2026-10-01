package com.randevupazaryeri.push.entity;

/** Xcode debug builds get sandbox tokens; TestFlight / App Store builds get production tokens. */
public enum PushEnvironment {
    SANDBOX, PRODUCTION
}
