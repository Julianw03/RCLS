import {resolve} from "@/systems/LocalLinkResolver.ts";
import {LocalLink} from "@/types.ts";

const LoggedInHandler = () => {

    const performLogout = async () => {
        const resp = await fetch(resolve("/api/riotclient/login/v1/logout" as LocalLink), {
            method: "POST"
        });
        if (!resp.ok) {
            console.error("Failed to log out");
            return;
        }
    }

    return (
        <div>
            <p>You are logged in!</p>
            <button type={"button"} onClick={performLogout}>
                Log out
            </button>
        </div>
    )
}

export default LoggedInHandler;