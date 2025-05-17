const FadeIn = ({ children }: { children: React.ReactNode }) => {
    return (
        <div className="fade-in">
            {children}
            <style>{`
                .fade-in {
                    opacity: 0;
                    animation: fadeIn 0.5s forwards;
                }
                @keyframes fadeIn {
                    to {
                        opacity: 1;
                    }
                }
            `}</style>
        </div>
    );
}

export default FadeIn;