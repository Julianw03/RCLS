import './App.css'
import * as LocalLinkResolver from "./systems/LocalLinkResolver.ts";
import RessourceLoadingSystem from "./systems/RessourceLoadingSystem.ts";
import {LocalLink} from "./types.ts";
import DynamicBackground from "@/components/DynamicBackground.tsx";
import {useDispatch, useSelector} from "react-redux";
import {AppState} from "@/store.ts";
import LoginFlowHandler from "@/components/login/LoginFlowHandler.tsx";
import {useEffect, useState} from "react";
import {ACTION_SET_RCU_CONNECTED} from "@/store/RCUConnectionStateReducer.ts";
import {LoginState} from "@/store/LoginStateReducer.ts";
import SimpleSessionsLauncher from "@/components/sessionsLauncher/SimpleSessionsLauncher.tsx";
import {ToastContainer} from "react-toastify";
import BackgroundUploadDialog from "@/components/BackgroundUploadDialog.tsx";


const system = new RessourceLoadingSystem(
    {
        fetchUrl: new URL("https://raw.communitydragon.org/latest/plugins")
    }
)

system.load();


function App() {

    const dispatch = useDispatch();

    const rcuConnectionState = useSelector((state: AppState) => state.rcuConnectionState);
    const loginState = useSelector((state: AppState) => state.loginState);
    const [dialogOpen, setDialogOpen] = useState<boolean>(false);

    useEffect(() => {
        //Only check once on mount
        if (!rcuConnectionState.connected) {
            fetch(LocalLinkResolver.resolve("/api/connector/v1/parameters" as LocalLink)).then(
                (res) => {
                    if (!res.ok) {
                        fetch(LocalLinkResolver.resolve("/api/connector/v1/connect" as LocalLink), {
                            method: "POST"
                        })
                    }
                    dispatch(ACTION_SET_RCU_CONNECTED)
                }
            );
        }
    }, []);

    async function disconnectRcu() {
        await fetch(LocalLinkResolver.resolve("/api/connector/v1/disconnect" as LocalLink), {
            method: "POST"
        });
    }


    return (
        <div className={"rootContainer"}>
            {
                dialogOpen ? (
                    <BackgroundUploadDialog onClose={() => setDialogOpen(false)}/>
                ) : null
            }
            <div className={"mainDivider"}>
                <div className={"loginMediaSection"}>
                    <DynamicBackground/>
                </div>
                <div className={"loginDataSection"}>
                    <div>Riot Client Login Screens Prototype</div>
                    <button type={"button"}
                        onClick={disconnectRcu}
                        disabled={!rcuConnectionState.connected}
                    >
                        <span>Disconnect from Riot Client</span>
                    </button>

                    <div>Login State: {loginState}</div>
                    <LoginFlowHandler/>


                    {
                        loginState === LoginState.LOGGED_IN ? (
                            <SimpleSessionsLauncher/>
                        ) : null
                    }
                </div>
            </div>
            <ToastContainer position={"bottom-left"}/>
            <div className={"buttonContainer"}>
                <button type={"button"} onClick={() => setDialogOpen(true)}>
                    Change Background
                </button>
            </div>
        </div>
    )
}

export default App
