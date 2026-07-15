package donmani.donmani_server.fcm.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.YearMonth;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.PutObjectResult;

@ExtendWith(MockitoExtension.class)
class FortuneImageStorageServiceTest {

	@Mock
	private AmazonS3Client amazonS3Client;

	@Test
	void uploadStoresFortuneImageUnderFortuneContentPrefix() {
		FortuneImageStorageService storageService = new FortuneImageStorageService(amazonS3Client);
		ReflectionTestUtils.setField(storageService, "endPoint", "https://storage.example.com");
		ReflectionTestUtils.setField(storageService, "bucketName", "test-bucket");
		ReflectionTestUtils.setField(storageService, "storagePrefix", "fortune_content");
		when(amazonS3Client.putObject(any(PutObjectRequest.class))).thenReturn(new PutObjectResult());

		FortuneImageStorageService.StoredImage storedImage = storageService.upload(
			YearMonth.of(2026, 7),
			LocalDate.of(2026, 7, 1),
			new byte[] {1, 2, 3},
			"image/png"
		);

		assertThat(storedImage.key()).isEqualTo("fortune_content/2026-07/2026-07-01.png");
		assertThat(storedImage.url())
			.isEqualTo("https://storage.example.com/test-bucket/fortune_content/2026-07/2026-07-01.png");

		ArgumentCaptor<PutObjectRequest> putObjectRequest = ArgumentCaptor.forClass(PutObjectRequest.class);
		verify(amazonS3Client).putObject(putObjectRequest.capture());
		assertThat(putObjectRequest.getValue().getBucketName()).isEqualTo("test-bucket");
		assertThat(putObjectRequest.getValue().getKey()).isEqualTo("fortune_content/2026-07/2026-07-01.png");
		assertThat(putObjectRequest.getValue().getMetadata().getContentType()).isEqualTo("image/png");
	}

	@Test
	void uploadTreatsMissingJaxbAfterSuccessfulObjectCreationAsSuccess() {
		FortuneImageStorageService storageService = new FortuneImageStorageService(amazonS3Client);
		ReflectionTestUtils.setField(storageService, "endPoint", "https://storage.example.com");
		ReflectionTestUtils.setField(storageService, "bucketName", "test-bucket");
		ReflectionTestUtils.setField(storageService, "storagePrefix", "fortune_content");
		when(amazonS3Client.putObject(any(PutObjectRequest.class)))
			.thenThrow(new NoClassDefFoundError("javax/xml/bind/JAXBException"));
		when(amazonS3Client.doesObjectExist("test-bucket", "fortune_content/2026-07/2026-07-01.jpg"))
			.thenReturn(true);

		FortuneImageStorageService.StoredImage storedImage = storageService.upload(
			YearMonth.of(2026, 7),
			LocalDate.of(2026, 7, 1),
			new byte[] {1, 2, 3},
			"image/jpeg"
		);

		assertThat(storedImage.key()).isEqualTo("fortune_content/2026-07/2026-07-01.jpg");
		assertThat(storedImage.url())
			.isEqualTo("https://storage.example.com/test-bucket/fortune_content/2026-07/2026-07-01.jpg");
		verify(amazonS3Client).doesObjectExist("test-bucket", "fortune_content/2026-07/2026-07-01.jpg");
	}

	@Test
	void findStoredImageReturnsExistingObjectUrl() {
		FortuneImageStorageService storageService = new FortuneImageStorageService(amazonS3Client);
		ReflectionTestUtils.setField(storageService, "endPoint", "https://storage.example.com");
		ReflectionTestUtils.setField(storageService, "bucketName", "test-bucket");
		ReflectionTestUtils.setField(storageService, "storagePrefix", "fortune_content");
		when(amazonS3Client.doesObjectExist("test-bucket", "fortune_content/2026-08/2026-08-01.png"))
			.thenReturn(false);
		when(amazonS3Client.doesObjectExist("test-bucket", "fortune_content/2026-08/2026-08-01.jpg"))
			.thenReturn(true);

		FortuneImageStorageService.StoredImage storedImage = storageService.findStoredImage(
			YearMonth.of(2026, 8),
			LocalDate.of(2026, 8, 1)
		).orElseThrow();

		assertThat(storedImage.key()).isEqualTo("fortune_content/2026-08/2026-08-01.jpg");
		assertThat(storedImage.url())
			.isEqualTo("https://storage.example.com/test-bucket/fortune_content/2026-08/2026-08-01.jpg");
	}
}
