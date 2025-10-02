import {createAction, createReducer} from "@reduxjs/toolkit";

export interface RCUConnectionState {
    connected: boolean;
}


export const ACTION_SET_RCU_CONNECTION_STATE = createAction<boolean>("rcuConnectionState/setConnected");
export const ACTION_SET_RCU_CONNECTED = ACTION_SET_RCU_CONNECTION_STATE(true);
export const ACTION_SET_RCU_DISCONNECTED = ACTION_SET_RCU_CONNECTION_STATE(false);


const RcuConnectionStateReducer = createReducer(
    {connected: false} as RCUConnectionState,
    builder => {
        builder
            .addCase(
                ACTION_SET_RCU_CONNECTION_STATE,
                (state, action) => {
                    state.connected = action.payload;
                    return state;
                }
            )
    }
);

export default RcuConnectionStateReducer;