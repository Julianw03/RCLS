import {useEffect, useRef, useState} from "react";
import HCaptcha from "@hcaptcha/react-hcaptcha";
import * as Config from "../Config";
import {LocalLink} from "../types.ts";

export interface HCaptchaWrapperProps {
    siteKey: string;
    rqData: string;
    onVerify: (token: string, eKey: string) => void;
}

const HCaptchaWrapper = (
    {
        siteKey,
        rqData,
        onVerify,
    }: HCaptchaWrapperProps) => {
    const captchaRef = useRef<HCaptcha>(null);

    const handleClick = () => {
        captchaRef.current?.execute();
    };

    const [showButton, setShowButton] = useState(false);
    useEffect(() => {
        captchaRef.current?.setData({
            rqdata: rqData
        });
        console.log("rqdata changed", rqData);
    }, [rqData]);

    const onLoad = () => {
        captchaRef.current?.setData({
            rqdata: rqData
        });
        setShowButton(true);
    }


    return (
        <>
            {showButton ? <button onClick={() =>  {handleClick()}}>Log In</button> : <></>}
            <HCaptcha
                scriptSource={Config.resolveLocalLink("/hcaptcha-proxy/RCLS-INTERNAL/1/api.js" as LocalLink)}
                loadAsync={false}
                ref={captchaRef}
                sitekey={siteKey}
                onLoad={onLoad}
                onVerify={onVerify}
                size="invisible"
            />
        </>
    );
};

export default HCaptchaWrapper;