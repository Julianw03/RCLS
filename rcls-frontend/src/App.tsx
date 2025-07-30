import './App.css'
import { useRef, useState} from "react";
import * as LocalLinkResolver from "./systems/LocalLinkResolver.ts";
import RessourceLoadingSystem from "./systems/RessourceLoadingSystem.ts";
import {LocalLink} from "./types.ts";
import CustomHCaptchaWrapper from "./components/CustomHCaptchaWrapper.tsx";


const system = new RessourceLoadingSystem(
    {
        fetchUrl: new URL("https://raw.communitydragon.org/latest/plugins")
    }
)

system.load();

interface RQData {
    key: string;
    data: string | null;
}

function App() {

    const usernameInputRef = useRef<HTMLInputElement>(null);
    const passwordInputRef = useRef<HTMLInputElement>(null);

    const [captchaData, setCaptchaData] = useState<RQData | null>(null);

    const handleHcaptchaSubmit = async (token: string) => {
        const exampleUsername = usernameInputRef.current?.value;
        const examplePassword = passwordInputRef.current?.value;
        await fetch(LocalLinkResolver.resolve("/api/riotclient/login/v1/login" as LocalLink), {
            method: "POST",
            body: JSON.stringify({
                username: exampleUsername,
                password: examplePassword,
                remember: true,
                language: "en_US",
                captcha: "hcaptcha " + token
            }),
            headers: {
                "Content-Type": "application/json"
            }
        });
        usernameInputRef.current!.value = "";
        passwordInputRef.current!.value = "";
        setCaptchaData({...captchaData, data: null});
    }

    async function showClientUx() {
        await fetch(LocalLinkResolver.resolve("/api/riotclient/launcher/v1/client" as LocalLink), {
            method: "POST"
        });
    }


    return (
        <div className={"rootContainer"}>
            <div className={"mainDivider"}>
                <div className={"loginMediaSection"}>
                    <video className={"media"} autoPlay={true} loop={true} playsInline={true} controls={false}
                           disablePictureInPicture={true}>
                        <source
                            src={"https://raw.communitydragon.org/latest/plugins/rcp-be-lol-game-data/global/default/assets/characters/ahri/skins/skin86/animatedsplash/ahri_skin86_uncentered.skins_ahri_hol.webm"}/>
                    </video>
                    <audio autoPlay={true} controls={false} loop={true}>
                        <source
                            src={"https://raw.communitydragon.org/13.15/plugins/rcp-be-lol-game-data/global/default/assets/events/sfm2023marketing/sfx-sfmk-mus.ogg"}/>
                    </audio>
                </div>
                <div className={"loginDataSection"}>
                    <div>Riot Client Login Screens</div>
                    <button type={"button"} onClick={async () => {
                        await fetch(LocalLinkResolver.resolve("/api/connector/v1/connect" as LocalLink), {
                            method: "POST",
                        });
                    }}>
                        Connect Riot Client with RCLS
                    </button>
                    <button type={"button"} onClick={async () => {
                        const response = await fetch(LocalLinkResolver.resolve("/api/riotclient/login/v1/reset" as LocalLink), {
                            method: "POST"
                        });
                        if (Math.floor(response.status / 100) !== 2) {
                            console.error("Failed initial reset, will not fetch captcha data");
                            return;
                        }

                        await fetch(LocalLinkResolver.resolve("/api/riotclient/login/v1/captcha" as LocalLink))
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
                        captchaData && <CustomHCaptchaWrapper siteKey={captchaData.key} rqData={captchaData.data} onSuccess={(token) => handleHcaptchaSubmit(token)}/>
                    }
                    <br/>
                    <button type={"button"} onClick={showClientUx}>Show Client UX</button>
                </div>
            </div>
        </div>
    )
}

export default App
