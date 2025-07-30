import {useEffect, useRef} from "react";
import * as LocalLinkResolver from "@/systems/LocalLinkResolver.ts";
import {LocalLink} from "@/types.ts";
import {decode} from "@ygoe/msgpack";
import {useDispatch} from "react-redux";
import {ACTION_SET_BACKEND_CONNECTION_STATE, BackendConnectionState} from "@/store.ts";

interface GenericMessage<T> {
    service: string;
    type: string;
    data: T;
}

interface MapKeyMessage<K, V> extends GenericMessage<MapKeyData<K, V>> {
    type: "MapKeyFormat",
}

interface MapKeyData<K,V> {
    key: K
    value: V
    uri: string
}

interface StateUpdateMessage<T> extends GenericMessage<StateUpdateData<T>> {
    type: "StateUpdateFormat",
}

interface StateUpdateData<T> {
    state: T
    uri: string
}

const WSHandler = () => {

    const dispatch = useDispatch();

    const websocketRef = useRef<WebSocket | null>(null);

    useEffect(() => {
        if (websocketRef.current !== null) return;
        const ws = new WebSocket(LocalLinkResolver.resolve("/ws" as LocalLink, "wss"));
        websocketRef.current = ws;
        dispatch(ACTION_SET_BACKEND_CONNECTION_STATE(BackendConnectionState.CONNECTING))
        ws.binaryType = "arraybuffer";
        ws.onclose = (closeEvent: CloseEvent) => {
            dispatch(ACTION_SET_BACKEND_CONNECTION_STATE(BackendConnectionState.DISCONNECTED))
            console.log("WebSocket closed", closeEvent);
        }
        ws.onopen = () => {
            dispatch(ACTION_SET_BACKEND_CONNECTION_STATE(BackendConnectionState.CONNECTED))
            console.log("WebSocket opened");
        };
        ws.onmessage = (messageEvent) => {
            const message = decode(messageEvent.data) as GenericMessage<unknown>;
            handleMessage(message);
        };
        ws.onerror = (errorEvent) => {
            dispatch(ACTION_SET_BACKEND_CONNECTION_STATE(BackendConnectionState.ERROR))
            console.error("WebSocket error", errorEvent);
        }

        return () => {
            if (websocketRef.current) {
                websocketRef.current.close();
                websocketRef.current = null;
            }
        }
    }, []);

    const handleMessage = (msg: GenericMessage<unknown>) => {
        switch (msg.type) {
            case "MapKeyFormat": {
                const mapKeyMsg = msg as MapKeyMessage<unknown, unknown>;
                console.log(mapKeyMsg.service, mapKeyMsg.data.uri, mapKeyMsg.data.key, mapKeyMsg.data.value);
                break;
            }
            case "StateUpdateFormat": {
                const stateUpdateMsg = msg as StateUpdateMessage<unknown>;
                console.log(stateUpdateMsg.service, stateUpdateMsg.data.uri, stateUpdateMsg.data.state)
                break;
            }
            default:
                console.log("Unknown message type:", msg.type);
        }
    }

    return (
        <></>
    );
}

export default WSHandler;