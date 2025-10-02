import * as LocalLinkResolver from "@/systems/LocalLinkResolver.ts";
import {LocalLink} from "@/types.ts";
import CustomHCaptchaWrapper from "@/components/CustomHCaptchaWrapper.tsx";
import {useRef} from "react";
import {useQuery} from "@tanstack/react-query";


interface RQData {
    key: string;
    data: string | null;
}


const fetchCaptchaData = async (): Promise<RQData> => {
    const resetRes = await fetch(LocalLinkResolver.resolve("/api/riotclient/login/v1/reset" as LocalLink), {
        method: "POST",
    });
    if (!resetRes.ok) {
        throw new Error("Failed initial reset, will not fetch captcha data");
    }

    const captchaRes = await fetch(LocalLinkResolver.resolve("/api/riotclient/login/v1/captcha" as LocalLink));
    if (!captchaRes.ok) {
        throw new Error("Failed to fetch captcha data");
    }
    return captchaRes.json();
};

const LoggedOutHandler = () => {
    const usernameInputRef = useRef<HTMLInputElement>(null);
    const passwordInputRef = useRef<HTMLInputElement>(null);

    const {data: captchaData, error, isLoading, refetch} = useQuery({
        queryKey: ["captchaData"],
        queryFn: fetchCaptchaData,
        refetchOnWindowFocus: false,
        retry: true,
    });

    if (isLoading) return <div>Loading…</div>;
    if (error) return <div>Error loading captcha</div>;


    const handleHcaptchaSubmit = async (token: string) => {
        const exampleUsername = usernameInputRef.current?.value;
        const examplePassword = passwordInputRef.current?.value;
        const resp = await fetch(LocalLinkResolver.resolve("/api/riotclient/login/v1/login" as LocalLink), {
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
        if (Math.floor(resp.status / 100) !== 2) {
            console.error("Login failed", resp.status);
            throw new Error("Login failed with status " + resp.status);
        }

        return;
    }

    return (
        <>
            <button type={"button"} onClick={() => refetch()}>
                Get data / Refresh captcha data
            </button>
            <br/>
            <input type={"text"} ref={usernameInputRef} placeholder={"Username"}/>
            <br/>
            <input type={"password"} ref={passwordInputRef} placeholder={"Password"}/>
            {
                captchaData && <CustomHCaptchaWrapper siteKey={captchaData.key} rqData={captchaData.data!}
                                                      onSuccess={(token) => handleHcaptchaSubmit(token)}

                />
            }
        </>
    );
}

export default LoggedOutHandler;