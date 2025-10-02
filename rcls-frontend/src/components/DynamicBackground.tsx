import {useQuery} from "@tanstack/react-query";
import * as LocalLinkResolver from "../systems/LocalLinkResolver.ts";
import {LocalLink} from "@/types.ts";

enum BackgroundType {
    VIDEO = "VIDEO",
    IMAGE = "IMAGE",
}

const fetchBackgroundInfo = async () => {
    const response = await fetch(LocalLinkResolver.resolve("/api/rcls/media/v1/background/type" as LocalLink));
    if (!response.ok) {
        throw new Error("Network response was not ok");
    }
    return response.json();
}

const DynamicBackground = () => {
    const {data, error, isLoading} = useQuery<BackgroundType>({
        queryKey: ["backgroundInfo"],
        queryFn: fetchBackgroundInfo,
    });

    if (isLoading || error || data === undefined) return <div></div>

    console.log("Background type: ", data);

    switch (data) {
        case BackgroundType.VIDEO:
            return (
                <video className={"media"} autoPlay loop muted>
                    <source src={LocalLinkResolver.resolve("/api/rcls/media/v1/background" as LocalLink)}/>
                </video>
            )
        case BackgroundType.IMAGE:
            return (
                <img className={"media"} src={LocalLinkResolver.resolve("/api/rcls/media/v1/background" as LocalLink)}
                     alt={"Background"}/>
            )
        default:
            console.warn("Unknown background type: ", data);
            return <div></div>
    }
}

export default DynamicBackground;