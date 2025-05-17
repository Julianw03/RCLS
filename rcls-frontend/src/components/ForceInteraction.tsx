import React from "react";
import FadeIn from "./FadeIn.tsx";

export interface ForceInteractionProps {
    children: React.ReactNode;
}

const ForceInteraction = ({children}: ForceInteractionProps) => {
    const [hasInteracted, setHasInteracted] = React.useState(false);
    const handleInteraction = () => {
        setHasInteracted(true);
    };

    if (!hasInteracted) {
        return (
            <div onClick={handleInteraction}
                 style={{
                     position: 'fixed',
                     top: 0,
                     left: 0,
                     width: '100%',
                     height: '100%',
                     backgroundColor: 'rgba(0, 0, 0, 0.5)',
                     display: 'flex',
                     alignItems: 'center',
                     justifyContent: 'center',
                     zIndex: 9999,
                     color: 'white',
                     textAlign: 'center',
                     cursor: 'pointer',
                     userSelect: 'none',
                 }}>
                <div>
                    <p>Click anywhere to enable interactions.</p>
                    <p>This is a one-time action.</p>
                </div>
            </div>
        );
    }

    return <FadeIn>{children}</FadeIn>;
}

export default ForceInteraction;