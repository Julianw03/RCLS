package com.julianw03.rcls.Util.state;

public interface StateListener<T> {
    void onStateChanged(T previousState, T newState);

    void initialize(T initialState);
}
