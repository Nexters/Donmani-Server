package donmani.donmani_server.reward.repository;

import donmani.donmani_server.reward.entity.RewardItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface RewardItemRepository extends JpaRepository<RewardItem, Long> {
    @Query("SELECT r FROM RewardItem r WHERE r.isHidden = true ORDER BY r.id ASC")
    Optional<RewardItem> findFirstByHiddenTrue();

    @Query("SELECT r FROM RewardItem r WHERE r.isHidden = false")
    List<RewardItem> findAllVisibleItems();
}
