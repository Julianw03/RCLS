import {useDispatch, useSelector} from "react-redux";
import {AppState} from "@/store.ts";
import * as LocalLinkResolver from "@/systems/LocalLinkResolver.ts";
import {resolve} from "@/systems/LocalLinkResolver.ts";
import {ACTION_SET_LOGIN_STATE, LoginState} from "@/store/LoginStateReducer.ts";
import {LocalLink} from "@/types.ts";
import LoggedOutHandler from "@/components/login/LoggedOutHandler.tsx";
import LoggedInHandler from "@/components/login/LoggedInHandler.tsx";
import MultifactorHandler from "@/components/login/MultifactorHandler.tsx";

const LoginFlowHandler = () => {

    const dispatch = useDispatch();
    const loginState = useSelector((app: AppState) => app.loginState)

    if (loginState === null || loginState === undefined || loginState === LoginState.UNKNOWN) {
        return (
            <div>
                <button type={"button"} onClick={async () => {
                    const resp = await fetch(resolve("/api/riotclient/login/v1/status" as LocalLink));
                    if (!resp.ok) {
                        console.error("Failed to fetch login state");
                        return;
                    }

                    const data = await resp.json();
                    dispatch(ACTION_SET_LOGIN_STATE(data))
                }}>
                    Manually fetch login state
                </button>
            </div>
        )
    }

    switch (loginState) {
        case LoginState.LOGGED_IN:
            return <LoggedInHandler/>
        case LoginState.MULTIFACTOR_REQUIRED:
            return (
                <MultifactorHandler/>
            );
        case LoginState.LOGGED_OUT:
            return <LoggedOutHandler/>;
        case LoginState.ERROR:

            fetch(LocalLinkResolver.resolve("/api/riotclient/login/v1/reset" as LocalLink), {
                method: "POST"
            });

            return (
                <div>
                    An error occured and the login state is trying to be reset. This should only take a few seconds.
                </div>
            )
    }

}

export default LoginFlowHandler;