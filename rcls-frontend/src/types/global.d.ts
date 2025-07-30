export {};

declare global {
    interface Window {
        hcaptcha: {
            execute: (widgetId?: string, options: { async: boolean }) => void | Promise<{response: string, key: string}>;
            reset: (widgetId?: string) => void;
            render: (container: string | HTMLElement, params: {
                theme?: string;
                sitekey: string;
                size?: 'normal' | 'compact' | 'invisible';
            }) => string;
            setData: (data: {rqdata: string}) => void;
        };
    }
}