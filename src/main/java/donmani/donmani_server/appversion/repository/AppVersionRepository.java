package donmani.donmani_server.appversion.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import donmani.donmani_server.appversion.entity.PlatformType;
import donmani.donmani_server.appversion.entity.AppVersion;

public interface AppVersionRepository extends JpaRepository<AppVersion, Long> {
	@Query("SELECT a FROM AppVersion a WHERE a.platformType = :platformType ORDER BY a.id DESC LIMIT 1")
	AppVersion findLatestVersionByPlatform(@Param("platformType") PlatformType platformType);
}
