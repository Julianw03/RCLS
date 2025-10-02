import {createAction, createReducer} from "@reduxjs/toolkit";

export enum BackendConnectionState {
    DISCONNECTED = "DISCONNECTED",
    CONNECTING = "CONNECTING",
    CONNECTED = "CONNECTED",
    ERROR = "ERROR"
}


export const ACTION_SET_BACKEND_CONNECTION_STATE = createAction<BackendConnectionState>("backendConnectionState/set");
export const ACTION_RESET_BACKEND_CONNECTION_STATE = createAction("backendConnectionState/reset");

const WSConnectionStateReducer = createReducer<BackendConnectionState>(
    BackendConnectionState.DISCONNECTED,
    builder => {
        builder
            .addCase(
                ACTION_SET_BACKEND_CONNECTION_STATE,
                (_, action) => action.payload
            )
            .addCase(
                ACTION_RESET_BACKEND_CONNECTION_STATE,
                () => BackendConnectionState.DISCONNECTED
            )
    }
)

export default WSConnectionStateReducer;