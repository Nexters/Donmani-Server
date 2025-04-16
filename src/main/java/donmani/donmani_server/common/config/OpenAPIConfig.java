package donmani.donmani_server.common.config;

import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

public class OpenAPIConfig {
	/**
	 * OpenAPI bean 구성
	 * @return
	 */
	@Bean
	public OpenAPI openAPI() {
		Info info = new Info()
			.title("swagger 테스트")
			.version("1.0")
			.description("API에 대한 설명 부분");

		Server server = new Server();
		server.setUrl("https://www.donmani.kr");
		return new OpenAPI()
			.components(new Components())
			.info(info).addServersItem(server);
	}
}