import {createAction, createReducer} from "@reduxjs/toolkit";

export enum LoginState {
    LOGGED_OUT = "LOGGED_OUT",
    LOGGED_IN = "LOGGED_IN",
    MULTIFACTOR_REQUIRED = "MULTIFACTOR_REQUIRED",
    ERROR = "ERROR",
    UNKNOWN = "UNKNOWN"
}


export const ACTION_SET_LOGIN_STATE = createAction<LoginState>("loginState/set");
export const ACTION_RESET_LOGIN_STATE = createAction("loginState/reset");


const LoginStateReducer = createReducer<LoginState>(
    LoginState.UNKNOWN,
    builder => {
        builder
            .addCase(
                ACTION_SET_LOGIN_STATE,
                (_, action) => action.payload
            )
            .addCase(
                ACTION_RESET_LOGIN_STATE,
                () => LoginState.UNKNOWN
            )
    }
)

export default LoginStateReducer;