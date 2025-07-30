import {configureStore, createAction, createReducer, EnhancedStore} from "@reduxjs/toolkit";

export interface AppState {
    backendConnectionState: BackendConnectionState;
}

export enum BackendConnectionState {
    DISCONNECTED = "DISCONNECTED",
    CONNECTING = "CONNECTING",
    CONNECTED = "CONNECTED",
    ERROR = "ERROR"
}

export const ACTION_SET_BACKEND_CONNECTION_STATE = createAction<BackendConnectionState>("backendConnectionState/set");
export const ACTION_RESET_BACKEND_CONNECTION_STATE = createAction("backendConnectionState/reset");

const BackendConnectionStateReducer = createReducer<BackendConnectionState>(
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


const appReducer = () => {
    return {
        backendConnectionState: BackendConnectionStateReducer
    }
}

const store: EnhancedStore<AppState> = configureStore({
    reducer: appReducer(),
})

export default store;