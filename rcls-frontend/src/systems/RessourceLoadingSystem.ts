export interface ResourceLoadingSystemArgs {
    fetchUrl: URL
}

interface SystemConfig {
    videos: {
        [key: string]: {
            url: string
        }
    }
    images: {
        [key: string]: {
            url: string
        }
    }
    audios: {
        [key: string]: {
            url: string
        }
    }
}

interface MapEntry {
    loadState: LoadState;
    promise?: Promise<string>;
    blobUrl?: string;
}

enum LoadState {
    REQUESTED,
    LOADED
}

class RessourceLoadingSystem {
    static instance: RessourceLoadingSystem | null = null;

    static getInstance = () => {
        if (this.instance === null) {
            throw new Error("RessourceLoadingSystem not initialized");
        }
        return this.instance;
    }

    private log(...message: unknown[]) {
        console.log("ResourceLoadingSystem: ", ...message);
    }

    private info(...message: unknown[]) {
        console.info("ResourceLoadingSystem: ", ...message);
    }

    
    // eslint-disable-next-line @typescript-eslint/ban-ts-comment
    // @ts-expect-error
    private warn(...message: unknown[]) {
        console.warn("ResourceLoadingSystem: ", ...message);
    }

    private error(...message: unknown[]) {
        console.error("ResourceLoadingSystem: ", ...message);
    }

    private readonly fetchUrl: URL;

    private readonly audioMap: Map<string, MapEntry> = new Map();
    private readonly imageMap: Map<string, MapEntry> = new Map();
    private readonly videoMap: Map<string, MapEntry> = new Map();

    constructor(
        {
            fetchUrl
        }: ResourceLoadingSystemArgs) {
        this.fetchUrl = fetchUrl;
        RessourceLoadingSystem.instance = this;
    }

    public getDirectVideoUrl = (key: string): string | undefined => {
        const entry = this.videoMap.get(key);
        if (entry && entry.loadState === LoadState.LOADED) {
            return entry.blobUrl;
        }
        return undefined;
    }

    public load = async () => {
        const config: SystemConfig = {
            videos: {
                "background": {
                    url: `${this.fetchUrl}/rcp-be-lol-game-data/global/default/assets/characters/ahri/skins/skin86/animatedsplash/ahri_skin86_uncentered.skins_ahri_hol.webm`
                }
            },
            images: {},
            audios: {}
        }
        this.info("Fetched config: ", config);
        this.log("Loading Videos");
        const videoPromises = Promise.allSettled(Object.entries(config.videos).map(([key, value]) => {
            const promise = this.loadRessource(value.url);
            this.videoMap.set(key, {
                loadState: LoadState.REQUESTED,
                promise: promise
            });
            return promise.then((blobUrl => {
                this.videoMap.set(key, {
                    loadState: LoadState.LOADED,
                    blobUrl: blobUrl
                });
                return blobUrl;
            }));
        })).then(() => {
            this.info("All video resources loaded");
        });
        this.log("Loading Images");
        const imagePromises = Promise.allSettled(Object.entries(config.images).map(([key, value]) => {
            const promise = this.loadRessource(value.url);
            this.imageMap.set(key, {
                loadState: LoadState.REQUESTED,
                promise: promise
            });
            return promise.then((blobUrl => {
                this.imageMap.set(key, {
                    loadState: LoadState.LOADED,
                    blobUrl: blobUrl
                });
                return blobUrl;
            }));
        })).then(() => {
            this.info("All image resources loaded");
        });
        this.log("Loading Audios");
        const audioPromises = Promise.allSettled(Object.entries(config.audios).map(([key, value]) => {
            const promise = this.loadRessource(value.url);
            this.audioMap.set(key, {
                loadState: LoadState.REQUESTED,
                promise: promise
            });
            return promise.then((blobUrl => {
                this.audioMap.set(key, {
                    loadState: LoadState.LOADED,
                    blobUrl: blobUrl
                });
                return blobUrl;
            }));
        })).then(() => {
            this.info("All audio resources loaded");
        });
        await Promise.allSettled([
            videoPromises,
            imagePromises,
            audioPromises
        ])
    }

    private loadRessource = async (url: string) => {
        const resourceBlob = await fetch(url)
            .then((resp) => resp.blob())
            .catch((err) => {
                this.error(`Error fetching resource: ${url}`, err);
                throw err;
            });

        const objectURL = URL.createObjectURL(resourceBlob);
        this.info("Fetched resource: ", url, " -> Object URL:", objectURL);
        return objectURL;
    }
}

export default RessourceLoadingSystem;