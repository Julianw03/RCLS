import {LocalLink} from "./types.ts";
import {isNil} from "./Utils.ts";

const DEV_BACKEND_PORT: string | undefined = import.meta.env.VITE_BACKEND_PORT;
const DEFAULT_SSL_PORT = '443';

const createBackendUrl = (backendPort: string | undefined) => {
    if (isNil(backendPort)) {
        return "";
    }
    const backendUrl = `https://127.0.0.1`;
    if (DEFAULT_SSL_PORT === backendPort) {
        return backendUrl;
    }

    return `${backendUrl}:${backendPort}`;
}


export const BACKEND_URL = createBackendUrl(DEV_BACKEND_PORT);

export const resolveLocalLink = (localLink: LocalLink) => {
    if (isNil(DEV_BACKEND_PORT)) {
        // We are not in a dev environment, backend and frontend server are the same, so we can
        // just return the relative path
        return localLink as string;
    }
    return `${BACKEND_URL}${localLink}`;
}
