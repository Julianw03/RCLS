import {isNil} from "../Utils.ts";
import {LocalLink} from "../types.ts";
import {RCLS_BACKEND_PORT} from "../Config.ts";

const DEFAULT_SSL_PORT = '443';

const protocolToUrlMap = new Map();

const log = (...data: unknown[]) => {
    console.log("[Local-Link-Resolver]", ...data);
}

const registerProtocol = (protocol: string) => {
    const val = createBackendUrl(protocol, RCLS_BACKEND_PORT);
    log("Register", protocol , "->", val)
    protocolToUrlMap.set(protocol, val);
    return val;
}

const getBackendUrl = (protocol: string) => {
    const val = protocolToUrlMap.get(protocol);
    if (!isNil(val)) {
        return val;
    }
    log("Get called for unregistered protocol, will register", protocol)
    return registerProtocol(protocol);
}


const createBackendUrl = (protocol: string, backendPort: string | undefined) => {
    if (isNil(backendPort)) {
        return `${protocol}://${window.location.hostname}`
    }
    const backendUrl = `${protocol}://127.0.0.1`;
    if (DEFAULT_SSL_PORT === backendPort) {
        return backendUrl;
    }

    return `${backendUrl}:${backendPort}`;
}

export const resolve = (localLink: LocalLink, protocol: string = "https") => {
    return `${getBackendUrl(protocol)}${localLink}`;
}

registerProtocol("https");
registerProtocol("wss");