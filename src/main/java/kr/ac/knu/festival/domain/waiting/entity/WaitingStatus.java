package kr.ac.knu.festival.domain.waiting.entity;

import java.util.EnumSet;
import java.util.Set;

public enum WaitingStatus {
    WAITING,
    CALLED,
    ENTERED,
    SKIPPED,
    CANCELLED;

    private static final Set<WaitingStatus> ACTIVE = EnumSet.of(WAITING, CALLED);

    public boolean isActive() {
        return ACTIVE.contains(this);
    }

    public boolean canCall() {
        return this == WAITING;
    }

    public boolean canEnter() {
        return this == CALLED || this == WAITING;
    }

    public boolean canSkip() {
        return this == CALLED;
    }

    public boolean canCancel() {
        return this == WAITING || this == CALLED;
    }
}
