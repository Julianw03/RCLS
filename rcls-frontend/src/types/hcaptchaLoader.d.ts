declare module '@hcaptcha/loader' {
    interface IScriptParams {
        sentry?: boolean;
        scriptLocation?: HTMLElement;
        secureApi?: boolean;
        scriptSource?: string;
        apihost?: string;
        loadAsync?: boolean;
        cleanup?: boolean;
        query?: string;
        crossOrigin?: string;
    }

    function hCaptchaLoader(params: IScriptParams): Promise<>;
}