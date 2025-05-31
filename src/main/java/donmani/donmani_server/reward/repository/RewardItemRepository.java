package donmani.donmani_server.reward.repository;

import donmani.donmani_server.reward.entity.RewardItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RewardItemRepository extends JpaRepository<RewardItem, Long> {
    Optional<RewardItem> findFirstByHiddenTrue();

    List<RewardItem> findAllByHiddenFalse();
}
