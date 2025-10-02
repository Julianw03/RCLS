import {useEffect, useRef} from "react";
import * as LocalLinkResolver from "@/systems/LocalLinkResolver.ts";
import {LocalLink} from "@/types.ts";
import HCaptcha from "@hcaptcha/react-hcaptcha";

type Props = {
    siteKey: string;
    rqData?: string;
    onSuccess?: (token: string) => void;
};

const CustomHCaptchaWrapper = (
    {
        siteKey,
        rqData,
        onSuccess
    }: Props
) => {
    const captchaRef = useRef<HCaptcha>(null);

    const triggerCaptcha = () => {
        if (captchaRef.current) {
            captchaRef.current.execute({"async": false})
        }
    }

    useEffect(() => {
        setRQData();
    }, [rqData]);

    const setRQData = () => {
        if (captchaRef.current) {
            console.log("Set rqdata");
            captchaRef.current.setData({"rqdata": rqData})
        }
    }

    const resetHCaptcha = () => {
        if (captchaRef.current) {
            captchaRef.current.resetCaptcha();
            console.log("Reset HCaptcha");
        }
    }


    return (
        <>
            <HCaptcha
                sitekey={siteKey}
                onVerify={(token) => {
                    console.log("OnVerify", token);
                    onSuccess?.(token);
                }}
                scriptSource={LocalLinkResolver.resolve("/hcaptcha-proxy/RCLS-INTERNAL/1/api.js" as LocalLink)}
                ref={captchaRef}
                size={"invisible"}
                onExpire={() => console.log("Expired")}
                onLoad={() => {
                    setRQData();
                }}
                onChalExpired={() => console.log("Challenge Expired")}
            />
            <button role={"button"} onClick={() => triggerCaptcha()}>Login</button>
            <button role={"button"} onClick={() => resetHCaptcha()}>Reset</button>
        </>
    )
}

export default CustomHCaptchaWrapper;