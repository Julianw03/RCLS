import {Dispatch, useEffect, useRef} from "react";
import * as LocalLinkResolver from "@/systems/LocalLinkResolver.ts";
import {LocalLink} from "@/types.ts";
import {decode} from "@ygoe/msgpack";
import {useDispatch} from "react-redux";
import {ACTION_SET_BACKEND_CONNECTION_STATE, BackendConnectionState} from "@/store/WSConnectionStateReducer.ts";
import {UnknownAction} from "@reduxjs/toolkit";
import {ACTION_SET_LOGIN_STATE, LoginState} from "@/store/LoginStateReducer.ts";
import {
    ACTION_ADD_SESSION,
    ACTION_BULK_SET_SESSIONS,
    ACTION_REMOVE_SESSION,
    ProductId,
    SessionId
} from "@/store/SimpleSessionsReducer.ts";
import {ACTION_SET_RCU_CONNECTION_STATE} from "@/store/RCUConnectionStateReducer.ts";

interface GenericMessage<T> {
    source: string;
    payloadType: string;
    payload: T;
}

interface KeyViewUpdatedPayload<K, V> extends GenericMessage<MapKeyData<K, V>> {
    payloadType: "KeyViewUpdatedPayload",
}

interface MapKeyData<K, V> {
    key: K
    newValue: V
}

interface ViewUpdatedPayload<T> extends GenericMessage<ViewUpdatedData<T>> {
    payloadType: "ViewUpdatedPayload",
}

interface ViewUpdatedData<T> {
    newState: T
}

type Handler = (msg: GenericMessage<unknown>, dispatch: Dispatch<UnknownAction>) => void

const sourceHandlers: Map<string, Handler> = new Map();
sourceHandlers.set("RsoAuthenticationManager",
    (msg, dispatch) => {
        if (msg.payloadType !== "ViewUpdatedPayload") return;
        const typedMsg = msg as ViewUpdatedPayload<{ loginStatus: LoginState }>;
        const loginStatus = typedMsg.payload.newState.loginStatus;
        dispatch(ACTION_SET_LOGIN_STATE(loginStatus));
    });

sourceHandlers.set("SessionsManager", (msg, dispatch) => {
    switch (msg.payloadType) {
        case "KeyViewUpdatedPayload": {
            const typedMsg = msg as KeyViewUpdatedPayload<string, {
                patchlineFullName: string,
                productId: string
            } | null>;
            const key = typedMsg.payload.key;
            const newValue = typedMsg.payload.newValue;
            if (newValue === null) {
                console.log(`Session ended for ${key}`);
                dispatch(ACTION_REMOVE_SESSION(key as SessionId));
            } else {
                console.log(`Session started for ${key} on patchline ${newValue.patchlineFullName}`);
                dispatch(ACTION_ADD_SESSION({
                    sessionId: key as SessionId,
                    productId: newValue.productId as ProductId
                }))
            }
            break;
        }
        case "ViewUpdatedPayload": {
            const typedMsg = msg as ViewUpdatedPayload<Record<SessionId, {
                patchlineFullName: string,
                productId: ProductId
            }>>;
            const newState = typedMsg.payload.newState;
            console.log("Bulk session update", newState);

            const newReduxState = Object.entries(newState).reduce((acc, [sessionId, info]) => {
                const productId = info.productId;
                if (!acc[productId]) {
                    acc[productId] = [];
                }

                acc[productId].push(sessionId as SessionId);
                return acc;
            }, {} as Record<ProductId, SessionId[]>);

            dispatch(ACTION_BULK_SET_SESSIONS(newReduxState))
            break;
        }
    }
});

sourceHandlers.set("RCU", (msg, dispatch) => {
    const typedMessage = msg as GenericMessage<boolean>
    const payload = typedMessage.payload;

    dispatch(ACTION_SET_RCU_CONNECTION_STATE(payload));
})

const WSHandler = () => {

    const dispatch = useDispatch();

    const websocketRef = useRef<WebSocket | null>(null);

    useEffect(() => {
        let rcTimeout = null as NodeJS.Timeout | null;
        if (websocketRef.current !== null) return;

        const crateNewWebSocket = () => {
            const ws = new WebSocket(LocalLinkResolver.resolve("/ws" as LocalLink, "wss"));
            dispatch(ACTION_SET_BACKEND_CONNECTION_STATE(BackendConnectionState.CONNECTING))
            websocketRef.current = ws;
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
                if (!rcTimeout) {
                    rcTimeout = setTimeout(() => {
                        console.log("Reconnecting WebSocket...");
                        crateNewWebSocket();
                        rcTimeout = null;
                    }, 5_000);
                }
            }
        }

        crateNewWebSocket();

        return () => {
            if (websocketRef.current) {
                websocketRef.current.close();
                websocketRef.current = null;
            }
            if (rcTimeout) {
                clearTimeout(rcTimeout);
            }
        }
    }, []);

    const handleMessage = (msg: GenericMessage<unknown>) => {
        console.log(msg);
        sourceHandlers.get(msg.source)?.(msg, dispatch);
    }

    return (
        <></>
    );
}

export default WSHandler;