    package com.stayvida.backend.security;

    import org.springframework.http.HttpStatus;
    import org.springframework.http.ResponseEntity;

    import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

    public class ApiResponse {

        public static ResponseEntity<Map<String, Object>> success(Object data, String message) {
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("status", 200);
            response.put("message", message);
            response.put("data", data);
            return ResponseEntity.ok(response);
        }

        public static ResponseEntity<Map<String, Object>> created(Object data, String message) {
            Map<String, Object> response = new HashMap<>();
            response.put("status", 201);
            response.put("message", message);
            response.put("data", data);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        }

        public static ResponseEntity<Map<String, Object>> badRequest(String message) {
            Map<String, Object> response = new HashMap<>();
            response.put("status", 400);
            response.put("error", "Bad Request");
            response.put("message", message);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        public static ResponseEntity<Map<String, Object>> unauthorized(String message) {
            Map<String, Object> response = new HashMap<>();
            response.put("status", 401);
            response.put("error", "Unauthorized");
            response.put("message", message);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
        public static ResponseEntity<Map<String, Object>> notFound(String message) {
            Map<String, Object> response = new HashMap<>();
            response.put("status", 404);
            response.put("error", "Not Found");
            response.put("message", message);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }

        public static ResponseEntity<Map<String, Object>> serverError(String message) {
            Map<String, Object> response = new HashMap<>();
            response.put("status", 500);
            response.put("error", "Internal Server Error");
            response.put("message", message);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
        
        
    }
