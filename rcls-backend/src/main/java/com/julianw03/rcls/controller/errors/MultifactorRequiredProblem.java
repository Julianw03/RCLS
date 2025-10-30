package com.julianw03.rcls.controller.errors;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.julianw03.rcls.service.modules.rclient.login.model.MultifactorInfoDTO;
import com.julianw03.rcls.service.modules.rclient.login.model.MultifactorRequiredException;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.http.ProblemDetail;

@Schema(description = "Indicates that multifactor authentication is required to proceed.")
public record MultifactorRequiredProblem(
        @JsonUnwrapped
        ApiProblem baseProblem,
        MultifactorInfoDTO multifactorInfo
) {
    public static MultifactorRequiredProblem create(
            ProblemDetail base,
            MultifactorRequiredException multifactorRequiredException
    ) {
        ApiProblem apiProblem = ApiProblem.fromProblemDetail(base);
        MultifactorInfoDTO multifactorInfo = multifactorRequiredException.getMultifactorInfo();

        return new MultifactorRequiredProblem(
                apiProblem,
                multifactorInfo
        );
    }
}
