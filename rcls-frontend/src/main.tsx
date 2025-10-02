import {StrictMode} from 'react'
import {createRoot} from 'react-dom/client'
import './index.css'
import {Provider} from "react-redux";
import store from "./store.ts";
import ForceInteraction from "./components/ForceInteraction.tsx";
import WSHandler from "@/components/websocket/WSHandler.tsx";
import {QueryClient, QueryClientProvider} from "@tanstack/react-query";
import SimpleContextSwitcher from "./SimpleContextSwitcher.tsx";


createRoot(document.getElementById('root')!).render(
    <Provider store={store}>
        <WSHandler/>
        <StrictMode>
            <QueryClientProvider client={new QueryClient({
                defaultOptions: {
                    queries: {
                        staleTime: 0,
                        refetchOnWindowFocus: false
                    }
                }
            })}>
                <ForceInteraction>
                    <SimpleContextSwitcher/>
                </ForceInteraction>
            </QueryClientProvider>
        </StrictMode>
    </Provider>
)

