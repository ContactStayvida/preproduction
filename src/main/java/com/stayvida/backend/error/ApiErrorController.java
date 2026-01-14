package com.stayvida.backend.error;

import com.stayvida.backend.security.ApiResponse;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ApiErrorController implements ErrorController {

    @RequestMapping("/error")
    public ResponseEntity<?> handleError(HttpServletRequest request) {

        Object statusObj = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        Object pathObj = request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);

        int status = statusObj != null
                ? Integer.parseInt(statusObj.toString())
                : 500;

        String path = pathObj != null ? pathObj.toString() : "N/A";

        if (status == 404) {
            return ApiResponse.notFound(
                    "Endpoint does not exist: " + path);
        }

        if (status == 401) {
            return ApiResponse.unauthorized("Unauthorized access");
        }

        if (status == 400) {
            return ApiResponse.badRequest("Bad request");
        }

        return ApiResponse.serverError("Unexpected server error");
    }
}
