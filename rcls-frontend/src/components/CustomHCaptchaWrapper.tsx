import React from "react";

type Props = {
    siteKey: string;
    rqData?: string;
    onSuccess?: (token: string) => void;
};

class CustomHCaptchaWrapper extends React.Component<Props> {
    captchaRef = React.createRef<HTMLDivElement>();
    widgetId?: string;
    prevSiteKey?: string;

    componentDidMount() {
        this.renderCaptchaInitial();
        this.prevSiteKey = this.props.siteKey;
    }

    triggerCaptcha = async () => {
        if (!this.widgetId) {
            return
        }
        const promise = window.hcaptcha.execute(this.widgetId, {async: true}) as Promise<{
            response: string,
            key: string
        }>;
        promise.then(({response, key}) => {
            console.log("Captcha executed:", response, key);
            if (this.props.onSuccess) {
                this.props.onSuccess(response);
            }
        })
    };

    componentDidUpdate(prevProps: Props) {
        if (prevProps.rqData !== this.props.rqData) {
            console.log("rqData changed:", prevProps.rqData, "→", this.props.rqData);
            this.handleRqDataChange(this.props.rqData || "");
        }
    }

    handleRqDataChange(rqData: string) {
        console.log("Setting rqData:", rqData);
        window.hcaptcha.setData({rqdata: rqData});
    }

    componentWillUnmount() {
        if (this.widgetId !== undefined) {
            window.hcaptcha.reset(this.widgetId);
        }
    }

    renderCaptchaInitial() {
        if (this.captchaRef.current && !this.widgetId) {
            try {
                this.widgetId = window.hcaptcha.render(this.captchaRef.current, {
                    sitekey: "019f1553-3845-481c-a6f5-5a60ccf6d830",
                    size: "invisible"
                });
            } catch (e) {
                console.log("Failed to render hCaptcha:", e);
            }
            this.handleRqDataChange(this.props.rqData ?? "");
        }
    }

    render() {
        return (
            <>
                <div ref={this.captchaRef}/>
                <button role={"button"} onClick={() => (this.triggerCaptcha())}></button>
            </>
        )
    }
}

export default CustomHCaptchaWrapper;