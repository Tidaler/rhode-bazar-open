package com.rhodebazar.backend.web;

import com.rhodebazar.backend.exception.ConflitDonneesException;
import com.rhodebazar.backend.exception.RessourceIntrouvableException;
import com.rhodebazar.backend.exception.StockInsuffisantException;
import com.rhodebazar.backend.web.dto.ApiErrorResponse;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RessourceIntrouvableException.class)
    public ResponseEntity<ApiErrorResponse> gererIntrouvable(RessourceIntrouvableException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiErrorResponse.de(404, e.getMessage()));
    }

    @ExceptionHandler(StockInsuffisantException.class)
    public ResponseEntity<ApiErrorResponse> gererStockInsuffisant(StockInsuffisantException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiErrorResponse.de(409, e.getMessage()));
    }

    @ExceptionHandler(ConflitDonneesException.class)
    public ResponseEntity<ApiErrorResponse> gererConflit(ConflitDonneesException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiErrorResponse.de(409, e.getMessage()));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiErrorResponse> gererIdentifiantsInvalides(BadCredentialsException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiErrorResponse.de(401, "Identifiants invalides"));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> gererAccesRefuse(AccessDeniedException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiErrorResponse.de(403, "Acces refuse : privileges insuffisants"));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> gererValidation(MethodArgumentNotValidException e) {
        List<String> details = e.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + " : " + fe.getDefaultMessage())
                .toList();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiErrorResponse.de(400, "Donnees invalides", details));
    }
}
