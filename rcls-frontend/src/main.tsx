import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import './index.css'
import App from './App.tsx'
import {Provider} from "react-redux";
import store from "./store.ts";
import ForceInteraction from "./components/ForceInteraction.tsx";
import {hCaptchaLoader} from '@hcaptcha/loader';
import * as LocalLinkResolver from "./systems/LocalLinkResolver.ts";
import {LocalLink} from "./types.ts";
import WSHandler from "@/components/websocket/WSHandler.tsx";

hCaptchaLoader({
    sentry: false,
    scriptSource: LocalLinkResolver.resolve( "/hcaptcha-proxy/RCLS-INTERNAL/1/api.js" as LocalLink)
}).then(() => {
    createRoot(document.getElementById('root')!).render(
        <StrictMode>
            <Provider store={store}>
                <WSHandler />
                <ForceInteraction>
                    <App />
                </ForceInteraction>
            </Provider>
        </StrictMode>,
    )
})

