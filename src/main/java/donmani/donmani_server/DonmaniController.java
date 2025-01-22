package donmani.donmani_server;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import donmani.donmani_server.common.log.LogController;

@Controller
public class DonmaniController {
	@RequestMapping(value = "/")
	public String home() {
		LogController lc = new LogController();
		lc.logTest();
		return "home";
	}
}
