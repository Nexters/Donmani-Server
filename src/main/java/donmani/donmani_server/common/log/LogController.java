package donmani.donmani_server.common.log;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
public class LogController {
	@GetMapping("/log")
	public String logTest(){
		String name = "spring";

		//  {}는 쉼표 뒤에 파라미터가 치환되는 것
		log.error("error log={}", name);
		log.warn("warn log={}", name);
		log.info("info log={}", name);
		log.debug("debug log={}", name);
		log.trace("trace log={}", name);

		return "ok";
	}
}