package donmani.donmani_server.fcm.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import donmani.donmani_server.fcm.entity.FcmLog;

public interface FCMLogRepository extends JpaRepository<FcmLog, Long> { }

