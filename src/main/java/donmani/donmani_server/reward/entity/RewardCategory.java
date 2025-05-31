package donmani.donmani_server.reward.entity;

public enum RewardCategory {
    BACKGROUND("배경"),
    EFFECT("효과"),
    DECORATION("장식"),
    CASE("별통이"),
    BGM("배경음악"),
    ;

    private String name;

    private RewardCategory(String name) { this.name = name; }
}
