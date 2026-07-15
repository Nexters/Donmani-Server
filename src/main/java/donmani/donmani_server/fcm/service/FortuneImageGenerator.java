package donmani.donmani_server.fcm.service;

import donmani.donmani_server.fcm.entity.Fortune;
import donmani.donmani_server.fcm.entity.FortuneProvider;

public interface FortuneImageGenerator {
	FortuneProvider supports();

	GeneratedImagePayload generateImage(Fortune fortune);
}
