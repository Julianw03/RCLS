import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import './index.css'
import App from './App.tsx'
import {Provider} from "react-redux";
import store from "./store.ts";
import ForceInteraction from "./components/ForceInteraction.tsx";

createRoot(document.getElementById('root')!).render(
  <StrictMode>
      <Provider store={store}>
          <ForceInteraction>
              <App />
          </ForceInteraction>
      </Provider>
  </StrictMode>,
)
