package com.desafio.taskmanager.common.error;

import java.net.URI;
import java.time.Instant;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import com.desafio.taskmanager.common.web.TraceIdFilter;

/**
 * Handler global de erros. Toda resposta de erro da API e um ProblemDetail
 * (RFC 7807) com content-type application/problem+json.
 *
 * <p><b>Por que os handlers de infra do Spring estao aqui.</b> O
 * {@code @ExceptionHandler(Exception.class)} do fim desta classe e rede de
 * seguranca: pega o que sobrou. Por isso ele tambem capturava
 * {@code NoResourceFoundException}, {@code HttpRequestMethodNotSupportedException}
 * e {@code HttpMediaTypeNotSupportedException}, devolvendo <b>500</b> para o que
 * deveria ser 404, 405 e 415. Erro de navegacao virava erro de servidor. Cada
 * excecao abaixo tem handler proprio; o Spring escolhe pelo mais especifico.
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
    private static final URI METHOD_NOT_ALLOWED_TYPE = URI.create(BASE_TYPE + "metodo-nao-permitido");
    private static final URI UNSUPPORTED_MEDIA_TYPE = URI.create(BASE_TYPE + "tipo-nao-suportado");
    private static final URI MALFORMED_TYPE = URI.create(BASE_TYPE + "requisicao-malformada");
    private static final URI RULE_TYPE = URI.create(BASE_TYPE + "regra-de-negocio");
    private static final URI DATABASE_TYPE = URI.create(BASE_TYPE + "banco-indisponivel");
    private static final URI LLM_INVALID_TYPE = URI.create(BASE_TYPE + "resposta-llm-invalida");
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

    /**
     * 404 de rota inexistente. O Spring 7 lanca {@code NoResourceFoundException}
     * para recurso estatico ou endpoint nao mapeado; sem este handler o
     * catch-all devolvia 500.
     */
    @ExceptionHandler({NoResourceFoundException.class, NoHandlerFoundException.class})
    public ProblemDetail handleNoHandler(Exception ex) {
        return problem(HttpStatus.NOT_FOUND, NOT_FOUND_TYPE, "Recurso nao encontrado",
                "Recurso nao encontrado");
    }

    /** 405: o metodo HTTP existe na rota mas nao e aceito. */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ProblemDetail handleMethodNotAllowed(HttpRequestMethodNotSupportedException ex) {
        return problem(HttpStatus.METHOD_NOT_ALLOWED, METHOD_NOT_ALLOWED_TYPE,
                "Metodo nao permitido", "Metodo HTTP nao suportado para este recurso");
    }

    /** 415: Content-Type do corpo nao suportado pela rota. */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ProblemDetail handleUnsupportedMediaType(HttpMediaTypeNotSupportedException ex) {
        return problem(HttpStatus.UNSUPPORTED_MEDIA_TYPE, UNSUPPORTED_MEDIA_TYPE,
                "Tipo nao suportado", "Content-Type nao suportado");
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

    /**
     * ERR-06: falha de persistencia (conexao perdida, constraint violada,
     * deadlock). O status 500 ja seria o correto pelo catch-all, mas um handler
     * explicito documenta a intencao e permite trocar o comportamento sem
     * depender da rede de seguranca. SQL e nome de tabela ficam so no log.
     */
    @ExceptionHandler(DataAccessException.class)
    public ProblemDetail handleDataAccess(DataAccessException ex) {
        log.error("Falha de acesso ao banco", ex);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, DATABASE_TYPE, "Erro interno",
                GENERIC_500_MESSAGE);
    }

    /**
     * ERR-04: a resposta do LLM continuou fora do contrato depois do retry e o
     * adaptador lancou {@link InvalidLlmResponseException} (T-F03-03).
     *
     * <p>502 (bad gateway): o servidor chamou um servico externo e recebeu
     * resposta invalida. O codigo {@code LLM_INVALID_RESPONSE} entra como
     * propriedade identificadora do cenario — o {@code type} segue o padrao em
     * portugues do resto da classe.
     *
     * <p>O detalhe e fixo: a mensagem da excecao carrega o motivo do validador
     * e pode conter trecho do JSON devolvido pelo modelo, que e interno. O
     * motivo real vai para o log, como em todo 5xx daqui.
     */
    @ExceptionHandler(InvalidLlmResponseException.class)
    public ProblemDetail handleInvalidLlmResponse(InvalidLlmResponseException ex) {
        log.error("Resposta do LLM fora do contrato", ex);
        ProblemDetail problem = problem(HttpStatus.BAD_GATEWAY, LLM_INVALID_TYPE,
                "Resposta invalida da IA", "O servico de IA devolveu uma resposta fora do contrato");
        problem.setProperty("code", "LLM_INVALID_RESPONSE");
        return problem;
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
        // instante em UTC, sempre no formato ISO-8601
        problem.setProperty("timestamp", Instant.now().toString());
        // vem do MDC colocado pelo TraceIdFilter; sem ele, a propriedade fica ausente
        String traceId = MDC.get(TraceIdFilter.TRACE_ID);
        if (traceId != null) {
            problem.setProperty(TraceIdFilter.TRACE_ID, traceId);
        }
        return problem;
    }
}