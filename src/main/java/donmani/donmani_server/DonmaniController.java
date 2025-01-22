package donmani.donmani_server;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class DonmaniController {
	@RequestMapping(value = "/")
	public String home() {
		return "home";
	}
}
