package donmani.donmani_server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class DonmaniServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(DonmaniServerApplication.class, args);
	}

}
