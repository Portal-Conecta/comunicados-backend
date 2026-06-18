import com.portal.conecta.comunicados.module.comunicado.domain.exception.AnnouncementNotFoundException;
import com.portal.conecta.comunicados.module.comunicado.domain.exception.AnnouncementMustBeInTheFutureException;
import com.portal.conecta.comunicados.module.comunicado.domain.exception.AnnouncementNotScheduledException;
import com.portal.conecta.comunicados.module.comunicado.domain.exception.AnnouncementPermissionDeniedException;

@ExceptionHandler(AnnouncementNotFoundException.class)
public ResponseEntity<ApiError> handleAnnouncementNotFound(
        AnnouncementNotFoundException exception,
        HttpServletRequest request
) {
    return buildResponse(HttpStatus.NOT_FOUND, exception, request);
}

@ExceptionHandler(AnnouncementMustBeInTheFutureException.class)
public ResponseEntity<ApiError> handleAnnouncementMustBeInTheFuture(
        AnnouncementMustBeInTheFutureException exception,
        HttpServletRequest request
) {
    return buildResponse(HttpStatus.BAD_REQUEST, exception, request);
}

@ExceptionHandler(AnnouncementNotScheduledException.class)
public ResponseEntity<ApiError> handleAnnouncementNotScheduled(
        AnnouncementNotScheduledException exception,
        HttpServletRequest request
) {
    return buildResponse(HttpStatus.CONFLICT, exception, request);
}

@ExceptionHandler(AnnouncementPermissionDeniedException.class)
public ResponseEntity<ApiError> handleAnnouncementPermissionDenied(
        AnnouncementPermissionDeniedException exception,
        HttpServletRequest request
) {
    return buildResponse(HttpStatus.FORBIDDEN, exception, request);
}