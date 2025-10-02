import {useRef, useState} from "react";
import {toast} from "react-toastify";
import {resolve} from "@/systems/LocalLinkResolver.ts";
import {LocalLink} from "@/types.ts";


const submitMfaCode = async (code: string, remember: boolean) => {
    const response = await fetch(resolve('/api/riotclient/login/v1/multifactor' as LocalLink), {
        method: 'POST',
        body: JSON.stringify({
            otp: code,
            rememberDevice: remember
        }),
        headers: {
            "Content-Type": "application/json"
        }
    });

    if (response.status !== 200 && response.status !== 204) {
        const errorText = await response.text();
        throw new Error(`Failed to submit MFA code: ${errorText}`);
    }

    return response;
}

const MultifactorHandler = () => {
    const inputRef = useRef<HTMLInputElement>(null);

    const [code, setCode] = useState<string>('');
    const [rememberMe, setRememberMe] = useState<boolean>(false);

    return (
        <div>
            <br/>
            <span>Multifactor Authentication Required</span>
            <input
                ref={inputRef}
                type="text"
                value={code}
                onChange={(e) => setCode(e.target.value)}
                placeholder="Enter MFA Code"
            />
            <br/>
            <input type="checkbox"
                   checked={rememberMe}
                   placeholder="Remember Me"
                   onChange={(e) => setRememberMe(e.target.checked)}
            />
            <span>Remember Me</span>
            <br/>
            <button
                onClick={() => {
                    toast.promise(
                        submitMfaCode(code, rememberMe),
                        {
                            pending: 'Submitting MFA Code...',
                            success: 'MFA Code submitted successfully!',
                            error: 'Failed to verify MFA Code'
                        }
                    )
                }}
            >
                Submit
            </button>
        </div>
    )
}


export default MultifactorHandler;