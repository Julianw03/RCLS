package com.julianw03.rcls.Util.state;

import com.julianw03.rcls.Util.Utils;

import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

public class State<T> {
    private final AtomicReference<T> stateRef;

    private Set<StateListener<T>> listeners;

    public State(T value) {
        this.stateRef = new AtomicReference<>(value);
        this.listeners = ConcurrentHashMap.newKeySet();
    }

    public State() {
        this(null);
    }

    public void setValue(T value) {
        T prev = stateRef.getAndSet(value);
        if (equalStates(prev, value)) {
            return;
        }
        notifyListeners(prev, value);
    }

    protected boolean equalStates(T state1, T state2) {
        return Objects.equals(state1, state2);
    }

    public T getValue() {
        return stateRef.get();
    }

    private void notifyListeners(T oldValue, T newValue) {
        for (StateListener<T> listener : listeners) {
            Utils.wrapSecure(() -> listener.onStateChanged(oldValue, newValue));
        }
    }

    public void addListener(StateListener<T> listener) {
        if (listener == null) {
            throw new IllegalArgumentException("Listener cannot be null");
        }
        listeners.add(listener);
        listener.initialize(stateRef.get());
    }

    public void removeListener(StateListener<T> listener) {
        if (listener == null) return;
        listeners.remove(listener);
    }
}
