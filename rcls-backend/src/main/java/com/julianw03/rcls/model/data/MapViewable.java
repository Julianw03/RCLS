package com.julianw03.rcls.model.data;

import java.util.Map;

public interface MapViewable<K, V> extends DataViewable<Map<K, V>> {
    V getViewForKey(Object key);
}
