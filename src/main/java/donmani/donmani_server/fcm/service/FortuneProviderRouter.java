package donmani.donmani_server.fcm.service;

import java.util.List;

import org.springframework.stereotype.Service;

import donmani.donmani_server.fcm.entity.FortuneProvider;

@Service
public class FortuneProviderRouter {

	private final List<FortuneTextGenerator> textGenerators;
	private final List<FortuneImageGenerator> imageGenerators;

	public FortuneProviderRouter(
		List<FortuneTextGenerator> textGenerators,
		List<FortuneImageGenerator> imageGenerators
	) {
		this.textGenerators = textGenerators;
		this.imageGenerators = imageGenerators;
	}

	public FortuneTextGenerator textGenerator(FortuneProvider provider) {
		return textGenerators.stream()
			.filter(generator -> generator.supports() == provider)
			.findFirst()
			.orElseThrow(() -> new IllegalArgumentException("지원하지 않는 텍스트 provider 입니다: " + provider));
	}

	public FortuneImageGenerator imageGenerator(FortuneProvider provider) {
		return imageGenerators.stream()
			.filter(generator -> generator.supports() == provider)
			.findFirst()
			.orElseThrow(() -> new IllegalArgumentException("지원하지 않는 이미지 provider 입니다: " + provider));
	}
}
