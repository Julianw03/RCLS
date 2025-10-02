import {useSelector} from "react-redux";
import {AppState} from "@/store.ts";
import {BackendConnectionState} from "@/store/WSConnectionStateReducer.ts";
import App from "@/App.tsx";

const SimpleContextSwitcher = () => {
    const wsConnectionState = useSelector((state: AppState) => state.wsConnectionState);

    if (wsConnectionState !== BackendConnectionState.CONNECTED) {
        return (
            <div className={"rootContainer"}>
                <h2>Awaiting WS Connection</h2>
                <h3>Current State: {BackendConnectionState[wsConnectionState]}</h3>
            </div>
        );
    }

    return <App/>
}

export default SimpleContextSwitcher;