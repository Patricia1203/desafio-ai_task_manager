package com.desafio.taskmanager.common.error;

import java.net.URI;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * Handler global de erros. Toda resposta de erro da API e um ProblemDetail
 * (RFC 7807) com content-type application/problem+json.
 *
 * Garantia central (ERR-02, ERR-06 e o requisito de nao vazar stack trace):
 * - a mensagem de 500 e sempre fixa, nunca vem da excecao original;
 * - a causa original so vai para o log do servidor;
 * - nenhuma propriedade da resposta carrega stack trace, classe de excecao,
 *   nome de tabela, SQL ou path de arquivo.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private static final String BASE_TYPE = "https://desafio.ai-task-manager/errors/";
    private static final URI VALIDATION_TYPE = URI.create(BASE_TYPE + "validacao");
    private static final URI NOT_FOUND_TYPE = URI.create(BASE_TYPE + "nao-encontrado");
    private static final URI MALFORMED_TYPE = URI.create(BASE_TYPE + "requisicao-malformada");
    private static final URI RULE_TYPE = URI.create(BASE_TYPE + "regra-de-negocio");
    private static final URI INTERNAL_TYPE = URI.create(BASE_TYPE + "erro-interno");

    private static final String GENERIC_500_MESSAGE =
            "Erro interno. Tente novamente mais tarde.";

    /** Bean Validation no corpo do request (POST/PUT). Lista os campos invalidos. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        List<FieldErrorItem> items = FieldErrorItem.from(ex.getBindingResult().getFieldErrors());

        ProblemDetail problem = problem(
                HttpStatus.BAD_REQUEST, VALIDATION_TYPE, "Requisicao invalida", "Falha de validacao");
        problem.setProperty("errors", items);
        return problem;
    }

    /** Bean Validation em parametros de path/query (@RequestParam, @PathVariable). */
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ProblemDetail handleHandlerMethodValidation(HandlerMethodValidationException ex) {
        List<FieldErrorItem> items = ex.getParameterValidationResults().stream()
                .flatMap(result -> result.getResolvableErrors().stream()
                        .map(error -> new FieldErrorItem(result.getMethodParameter().getParameterName(),
                                error.getDefaultMessage())))
                .toList();

        ProblemDetail problem = problem(
                HttpStatus.BAD_REQUEST, VALIDATION_TYPE, "Requisicao invalida", "Falha de validacao");
        problem.setProperty("errors", items);
        return problem;
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleNotFound(ResourceNotFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, NOT_FOUND_TYPE, "Recurso nao encontrado", ex.getMessage());
    }

    @ExceptionHandler(BusinessRuleException.class)
    public ProblemDetail handleBusinessRule(BusinessRuleException ex) {
        return problem(HttpStatus.UNPROCESSABLE_ENTITY, RULE_TYPE, "Regra de negocio violada", ex.getMessage());
    }

    /**
     * JSON malformado, enum de request invalido ou parametro ausente/truncated.
     * Os detalhes de Jackson ficam no log, nunca na resposta.
     */
    @ExceptionHandler({
            HttpMessageNotReadableException.class,
            MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class
    })
    public ProblemDetail handleMalformed(Exception ex) {
        log.debug("Requisicao malformada rejeitada", ex);
        return problem(HttpStatus.BAD_REQUEST, MALFORMED_TYPE, "Requisicao invalida",
                "Corpo ou parametro de requisicao invalido");
    }

    /** Qualquer erro nao mapeado. A causa vai para o log; a resposta e fixa. */
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception ex) {
        log.error("Erro inesperado nao tratado", ex);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, INTERNAL_TYPE,
                "Erro interno", GENERIC_500_MESSAGE);
    }

    private static ProblemDetail problem(HttpStatus status, URI type, String title, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        problem.setType(type);
        return problem;
    }
}