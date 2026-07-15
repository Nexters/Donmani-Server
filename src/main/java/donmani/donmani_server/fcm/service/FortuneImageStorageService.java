package donmani.donmani_server.fcm.service;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FortuneImageStorageService {

	private final AmazonS3Client amazonS3Client;

	@Value("${ncp.storage.endpoint}")
	private String endPoint;

	@Value("${ncp.storage.bucket-name}")
	private String bucketName;

	@Value("${ncp.storage.fortune-upload-dir}")
	private String storagePrefix;

	public StoredImage upload(
		YearMonth targetMonth,
		LocalDate targetDate,
		byte[] imageBytes,
		String mimeType
	) {
		String resolvedMimeType = StringUtils.hasText(mimeType) ? mimeType : "image/png";
		String extension = resolveExtension(resolvedMimeType);
		String keyName = buildKey(targetMonth, targetDate, extension);

		ObjectMetadata metadata = new ObjectMetadata();
		metadata.setContentLength(imageBytes.length);
		metadata.setContentType(resolvedMimeType);

		String normalizedBucketName = normalizeBucketName(bucketName);

		try (ByteArrayInputStream inputStream = new ByteArrayInputStream(imageBytes)) {
			amazonS3Client.putObject(
				new PutObjectRequest(normalizedBucketName, keyName, inputStream, metadata)
					.withCannedAcl(CannedAccessControlList.PublicRead)
			);
		} catch (NoClassDefFoundError e) {
			if (!isMissingJaxbError(e) || !doesObjectExist(normalizedBucketName, keyName)) {
				throw e;
			}
		} catch (Exception e) {
			throw new IllegalStateException("운세 이미지를 NCP Object Storage에 업로드하지 못했습니다.", e);
		}

		String imageUrl = buildUrl(normalizedBucketName, keyName);
		return new StoredImage(keyName, imageUrl);
	}

	public Optional<StoredImage> findStoredImage(
		YearMonth targetMonth,
		LocalDate targetDate
	) {
		String normalizedBucketName = normalizeBucketName(bucketName);
		for (String extension : new String[] {"png", "jpg", "webp"}) {
			String keyName = buildKey(targetMonth, targetDate, extension);
			if (doesObjectExist(normalizedBucketName, keyName)) {
				return Optional.of(new StoredImage(keyName, buildUrl(normalizedBucketName, keyName)));
			}
		}
		return Optional.empty();
	}

	private boolean doesObjectExist(
		String bucketName,
		String keyName
	) {
		try {
			return amazonS3Client.doesObjectExist(bucketName, keyName);
		} catch (Exception ignored) {
			return false;
		}
	}

	private boolean isMissingJaxbError(NoClassDefFoundError error) {
		return error.getMessage() != null && error.getMessage().contains("javax/xml/bind/JAXBException");
	}

	private String normalizeBucketName(String rawBucketName) {
		return rawBucketName == null ? "" : rawBucketName.replace("\"", "").trim();
	}

	private String resolveExtension(String mimeType) {
		return switch (mimeType) {
			case "image/jpeg" -> "jpg";
			case "image/webp" -> "webp";
			default -> "png";
		};
	}

	private String buildKey(
		YearMonth targetMonth,
		LocalDate targetDate,
		String extension
	) {
		return storagePrefix + "/" + targetMonth + "/" + targetDate + "." + extension;
	}

	private String buildUrl(
		String normalizedBucketName,
		String keyName
	) {
		return endPoint + "/" + normalizedBucketName + "/" + keyName;
	}

	public record StoredImage(
		String key,
		String url
	) {
	}
}
