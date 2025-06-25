package donmani.donmani_server.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class HiddenItemAlreadyOpenedException extends RuntimeException {
    public HiddenItemAlreadyOpenedException() {
        super("이미 히든 아이템을 열었습니다.");
    }
}
