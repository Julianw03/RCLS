import './App.css'
import { useRef, useState} from "react";
import * as Config from "./Config";
import RessourceLoadingSystem from "./systems/RessourceLoadingSystem.ts";
import HCaptchaWrapper from "./components/HCaptchaWrapper.tsx";


const system = new RessourceLoadingSystem(
    {
        fetchUrl: new URL("https://raw.communitydragon.org/latest/plugins")
    }
)

system.load();

interface RQData {
    key: string;
    data: string;
}

function App() {

    const usernameInputRef = useRef<HTMLInputElement>(null);
    const passwordInputRef = useRef<HTMLInputElement>(null);

    const [captchaData, setCaptchaData] = useState<RQData | null>(null);

    const handleHcaptchaSubmit = async (token: string) => {
        const exampleUsername = usernameInputRef.current?.value;
        const examplePassword = passwordInputRef.current?.value;
        await fetch(Config.BACKEND_URL + "/api/riotclient/login/v1/login", {
            method: "POST",
            body: JSON.stringify({
                username: exampleUsername,
                password: examplePassword,
                remember: false,
                language: "en_US",
                captcha: "hcaptcha " + token
            }),
            headers: {
                "Content-Type": "application/json"
            }
        });
        usernameInputRef.current!.value = "";
        passwordInputRef.current!.value = "";
    }

    async function showClientUx() {
        await fetch(Config.BACKEND_URL + "/api/riotclient/launcher/v1/client", {
            method: "POST"
        });
    }


    return (
        <div className={"rootContainer"}>
            <div className={"mainDivider"}>
                <div className={"loginMediaSection"}>
                    <video className={"media"} autoPlay={true} loop={true} playsInline={true} controls={false} disablePictureInPicture={true}>
                        <source src={"https://raw.communitydragon.org/latest/plugins/rcp-be-lol-game-data/global/default/assets/characters/ahri/skins/skin86/animatedsplash/ahri_skin86_uncentered.skins_ahri_hol.webm"}/>
                    </video>
                    <audio autoPlay={true} controls={false} loop={true}>
                        <source src={"https://raw.communitydragon.org/13.15/plugins/rcp-be-lol-game-data/global/default/assets/events/sfm2023marketing/sfx-sfmk-mus.ogg"}/>
                    </audio>
                </div>
                <div className={"loginDataSection"}>
                    <div>Riot Client Login Screens</div>
                    <button type={"button"} onClick={async () => {
                        await fetch(Config.BACKEND_URL + "/api/connector/v1/connect", {
                            method: "POST",
                        });
                    }}>
                        Connect Riot Client with RCLS
                    </button>
                    <button type={"button"} onClick={async () => {
                        const response = await fetch(Config.BACKEND_URL + "/api/riotclient/login/v1/reset", {
                            method: "POST"
                        });
                        if (Math.floor(response.status / 100) !== 2) {
                            console.error("Failed initial reset, will not fetch captcha data");
                            return;
                        }

                        await fetch(Config.BACKEND_URL + "/api/riotclient/login/v1/captcha")
                            .then((response) => response.json())
                            .then((json) => {
                                console.log(json);
                                setCaptchaData(json as RQData);
                            })
                    }}>
                        Get data
                    </button>
                    <br/>
                    <input type={"text"} ref={usernameInputRef} placeholder={"Username"}/>
                    <br/>
                    <input type={"password"} ref={passwordInputRef} placeholder={"Password"}/>
                    <br/>
                    {
                        captchaData && <HCaptchaWrapper siteKey={captchaData.key} rqData={captchaData.data}
                                                        onVerify={handleHcaptchaSubmit}/>
                    }
                    <br/>
                    <button type={"button"} onClick={showClientUx}>Show Client UX</button>
                </div>
            </div>
        </div>
    )
}

export default App
