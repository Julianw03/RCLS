import {createAction, createReducer} from "@reduxjs/toolkit";

export type ProductId = string & { __brand: "ProductId" };
export type SessionId = string & { __brand: "SessionId" };

export type SimpleSessionState = Record<ProductId, SessionId[]>

export interface SimpleSession {
    sessionId: SessionId;
    productId: ProductId;
}

export const ACTION_ADD_SESSION = createAction<SimpleSession>("simpleSessions/add");
export const ACTION_REMOVE_SESSION = createAction<SessionId>("simpleSessions/remove");

export const ACTION_BULK_SET_SESSIONS = createAction<SimpleSessionState>("simpleSessions/bulkSet");
export const ACTION_CLEAR_SESSIONS = createAction("simpleSessions/clear");

const SimpleSessionsReducer = createReducer(
    {} as SimpleSessionState,
    builder => {
        builder
            .addCase(
                ACTION_ADD_SESSION,
                (state, action) => {
                    const sessionId = action.payload.sessionId;
                    const productId = action.payload.productId;
                    if (state[productId] === undefined) {
                        state[productId] = [];
                    }
                    if (!state[productId].includes(sessionId)) {
                        state[productId].push(sessionId);
                    }
                    return state;
                }
            )
            .addCase(
                ACTION_REMOVE_SESSION,
                (state, action) => {
                    const sessionId = action.payload;
                    const keys = Object.keys(state) as ProductId[];
                    for (const key of keys) {
                        const index = state[key].indexOf(sessionId);
                        if (index !== -1) {
                            state[key].splice(index, 1);
                            if (state[key].length === 0) {
                                delete state[key];
                            }
                            break;
                        }
                    }
                    return state;
                }
            )
            .addCase(
                ACTION_BULK_SET_SESSIONS,
                (_, action) => {
                    return action.payload;
                }
            )
            .addCase(
                ACTION_CLEAR_SESSIONS,
                () => {
                    return {};
                }
            )
    }
);

export default SimpleSessionsReducer;
