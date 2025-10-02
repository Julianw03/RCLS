import {useQuery} from "@tanstack/react-query";
import {resolve} from "@/systems/LocalLinkResolver.ts";
import {LocalLink} from "@/types.ts";
import {toast} from "react-toastify";
import {useSelector} from "react-redux";
import {AppState} from "@/store.ts";

enum LookupStrategy {
    RCLS_INTERNAL = "RCLS_INTERNAL_NAME",
    RC_INTERNAL = "RIOT_INTERNAL_NAME",
    DISPLAY_NAME = "DISPLAY_NAME"
}

const killGame = async (gameId: string, lookupStrategy: LookupStrategy = LookupStrategy.DISPLAY_NAME) => {
    await fetch(resolve(`/api/riotclient/launcher/v1/game/${gameId}?lookupStrategy=${lookupStrategy}` as LocalLink), {
        method: "DELETE"
    });
}

const launchGame = async (gameId: string, lookupStrategy: LookupStrategy = LookupStrategy.DISPLAY_NAME) => {
    await fetch(resolve(`/api/riotclient/launcher/v1/game/${gameId}?lookupStrategy=${lookupStrategy}` as LocalLink), {
        method: "POST"
    });
}

const SimpleSessionsLauncher = () => {

    const lookupStrategy = LookupStrategy.DISPLAY_NAME;

    const sessions = useSelector((app: AppState) => app.simpleSessionState);
    const {data, error, isLoading} = useQuery<string[]>({
        queryKey: ["supportedGames"],
        queryFn: async () => {
            const response = await fetch(resolve(`/api/riotclient/launcher/v1/games/supported?lookupStrategy=${lookupStrategy}` as LocalLink));
            if (!response.ok) {
                throw new Error("Network response was not ok");
            }
            return response.json();
        }
    });

    if (isLoading || error || data === undefined) return <div></div>;
    console.log("Supported games: ", data);

    return (
        <>
            <br/>
            RCLS supports launching and killing the following games (does not mean they are installed):
            {data.map((game) => (
                <div key={game}>
                    <span>{game}</span>
                    <button onClick={() => {
                        toast.promise(
                            launchGame(game, lookupStrategy),
                            {
                                pending: `Launching ${game}...`,
                                success: `${game} launched!`,
                                error: `Error launching ${game}`
                            }
                        )
                    }}>Launch</button>
                    <button onClick={() => toast.promise(
                        killGame(game, lookupStrategy),
                        {
                            pending: `Killing ${game}...`,
                            success: `${game} killed!`,
                            error: `Error killing ${game}`
                        }
                    )}>Kill</button>
                </div>)
            )}
            <br/>
            Session Overview:
            {
                Object.entries(sessions).map(([productId, sessionIds]) => (
                    <div key={productId}>
                        <p>{productId} Has {sessionIds.length} active session(s).</p>
                    </div>
                ))
            }
        </>

    );
}

export default SimpleSessionsLauncher;