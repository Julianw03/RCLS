import {configureStore, EnhancedStore} from "@reduxjs/toolkit";
import LoginStateReducer, {LoginState} from "@/store/LoginStateReducer.ts";
import WSConnectionStateReducer, {BackendConnectionState} from "@/store/WSConnectionStateReducer.ts";
import SimpleSessionsReducer, {SimpleSessionState} from "@/store/SimpleSessionsReducer.ts";
import RcuConnectionStateReducer, {RCUConnectionState} from "@/store/RCUConnectionStateReducer.ts";

export interface AppState {
    wsConnectionState: BackendConnectionState;
    loginState: LoginState;
    simpleSessionState: SimpleSessionState;
    rcuConnectionState: RCUConnectionState;
}

const appReducer = () => {
    return {
        wsConnectionState: WSConnectionStateReducer,
        loginState: LoginStateReducer,
        simpleSessionState: SimpleSessionsReducer,
        rcuConnectionState: RcuConnectionStateReducer
    }
}

const store: EnhancedStore<AppState> = configureStore({
    reducer: appReducer(),
})

export default store;