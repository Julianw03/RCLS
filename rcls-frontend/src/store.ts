import {configureStore, createAction, createReducer, EnhancedStore} from "@reduxjs/toolkit";

export interface AppState {
    backendConnectionState: BackendConnectionState;
    hCaptchaLoadState: HCaptchaLoadState;
}

export enum HCaptchaLoadState {
    LOADING = "LOADING",
    LOADED = "LOADED",
    READY = "READY"
}

export enum BackendConnectionState {
    DISCONNECTED = "DISCONNECTED",
    CONNECTING = "CONNECTING",
    CONNECTED = "CONNECTED",
    ERROR = "ERROR"
}

export const ACTION_SET_BACKEND_CONNECTION_STATE = createAction<BackendConnectionState>("backendConnectionState/set");
export const ACTION_RESET_BACKEND_CONNECTION_STATE = createAction("backendConnectionState/reset");

export const ACTION_SET_HCAPTCHA_LOAD_STATE = createAction<HCaptchaLoadState>("hCaptchaLoadState/set");
export const ACTION_RESET_HCAPTCHA_LOAD_STATE = createAction("hCaptchaLoadState/reset");

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

const HCaptchaLoadStateReducer = createReducer<HCaptchaLoadState>(
    HCaptchaLoadState.LOADING,
    builder => {
        builder
            .addCase(
                ACTION_SET_HCAPTCHA_LOAD_STATE,
                (_, action) => action.payload
            )
            .addCase(
                ACTION_RESET_HCAPTCHA_LOAD_STATE,
                () => HCaptchaLoadState.LOADING
            )
    }
)

const appReducer = () => {
    return {
        backendConnectionState: BackendConnectionStateReducer,
        hCaptchaLoadState: HCaptchaLoadStateReducer,
    }
}

const store: EnhancedStore<AppState> = configureStore({
    reducer: appReducer(),
})

export default store;