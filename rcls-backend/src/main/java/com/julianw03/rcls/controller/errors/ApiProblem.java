package com.julianw03.rcls.controller.errors;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

@Schema(description = "Standard problem details as per RFC 7807")
public record ApiProblem(
        @Schema(description = "HTTP status code", format = "int32") int status,
        @Schema(description = "Short, human-readable title") String title,
        @Schema(description = "Detailed description") String detail,
        @Schema(description = "URI identifying the type of error", format = "uri") String type,
        @Schema(description = "Instance URI identifying the specific occurrence of the error", format = "uri") String instance
) {
    public static ApiProblem fromProblemDetail(ProblemDetail problemDetail) {
        return new ApiProblem(
                problemDetail.getStatus(),
                problemDetail.getTitle(),
                problemDetail.getDetail(),
                problemDetail.getType().toString(),
                problemDetail.getInstance() != null ? problemDetail.getInstance().toString() : null
        );
    }

    public static ApiProblem fromErrorResponseException(ErrorResponseException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
                ex.getStatusCode(),
                ex.getMessage()
        );
        return fromProblemDetail(pd);
    }
}
