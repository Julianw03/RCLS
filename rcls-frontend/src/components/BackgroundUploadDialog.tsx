import {toast} from "react-toastify";
import {resolve} from "@/systems/LocalLinkResolver.ts";
import {LocalLink} from "@/types.ts";
import {useState} from "react";

export interface BackgroundUploadDialogProps {
    onClose: () => void;
}

const uploadBackground = async (file: File) => {
    const formData = new FormData();
    formData.append("file", file);
    const response = await fetch(resolve("/api/rcls/media/v1/background" as LocalLink), {
        method: "POST",
        body: formData
    });
    if (!response.ok) {
        throw new Error("Failed to upload background");
    }
}

const BackgroundUploadDialog = (
    {
        onClose
    }: BackgroundUploadDialogProps
) => {

    const [file, setFile] = useState<File | null>(null);

    return (
        <div className={"uploadDialogBg"}>
            <div className={"outsideClickDetector"} onClick={onClose}/>
            <div className={"uploadDialog"}>
                <h2>Upload Background</h2>
                <div>
                    <input type={"file"} accept={"image/*,video/*"} onChange={async (e) => {
                        const file = e.target.files?.[0];
                        if (file) {
                            setFile(file);
                        }
                    }}/>
                    <br/>
                    <br/>
                    <button type={"button"} onClick={async () => {
                        if (!file) {
                            toast.error("No file selected");
                            return;
                        }
                        await toast.promise(
                            uploadBackground(file),
                            {
                                pending: 'Uploading background...',
                                success: 'Background uploaded successfully! Window will refresh to apply changes.',
                                error: 'Failed to upload background'
                            }
                        )
                        onClose();
                    }}>
                        Upload
                    </button>
                    <button type={"button"} onClick={onClose}>Cancel</button>
                </div>
            </div>
        </div>
    )
}

export default BackgroundUploadDialog;