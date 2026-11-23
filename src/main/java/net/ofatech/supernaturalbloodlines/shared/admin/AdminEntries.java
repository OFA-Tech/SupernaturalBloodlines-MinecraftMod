package net.ofatech.supernaturalbloodlines.shared.admin;

import static net.ofatech.supernaturalbloodlines.shared.admin.SettingKind.BOOL;
import static net.ofatech.supernaturalbloodlines.shared.admin.SettingKind.CHOICE;
import static net.ofatech.supernaturalbloodlines.shared.admin.SettingKind.DOUBLE;
import static net.ofatech.supernaturalbloodlines.shared.admin.SettingKind.INT;
import static net.ofatech.supernaturalbloodlines.shared.admin.SettingKind.LIST;
import static net.ofatech.supernaturalbloodlines.shared.admin.SettingKind.TEXT;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.Part;

/**
 * Every COMMON config key of ours, once: the allow-list of the admin path. The range, default and choices are read from the spec, so no
 * number is copied here. {@code AdminCoverageTest} fails the build for a key that is in a spec and not here, and for a row that no spec has.
 * Game rules we register would be in {@link #RULES}; there are none today.
 */
public final class AdminEntries {
    private static final String MOD = SupernaturalBloodlines.MODID;
    public static final String CORE_FILE = MOD + "-common.toml";
    public static final String VAMPIRISM_TENSURA_FILE = MOD + "-vampirism_tensura.toml";
    public static final String SUPERNATURAL_FILE = MOD + "-supernatural.toml";
    public static final String HUD_FILE = MOD + "-hud.toml";
    public static final String ARTIFACTS_FILE = MOD + "-artifacts.toml";
    public static final String SPECIES_FILE = MOD + "-species.toml";
    public static final String SPECIES_TENSURA_FILE = MOD + "-species_tensura.toml";
    public static final String CELESTIALS_FILE = MOD + "-celestials.toml";
    public static final String BOSSES_FILE = MOD + "-bosses.toml";

    /** The section ids, in the order the page lists them. */
    public static final List<String> SECTIONS = List.of("core", "vampirismTensura", "supernatural", "hud", "artifacts", "species", "speciesTensura", "celestials", "bosses");

    public static final String RESERVED = "reserved: the key does nothing";

    public static final List<SettingEntry> ALL;
    public static final List<SettingEntry> RULES = List.of();

    static {
        List<SettingEntry> all = new ArrayList<>();
        for (Part p : Part.values()) {
            all.add(SettingEntry.of(CORE_FILE, "core", "parts." + p.configKey(), BOOL).needsRestart());
        }
        all.add(SettingEntry.of(CORE_FILE, "core", "werewolves.full_form_level", INT));
        all.add(SettingEntry.of(CORE_FILE, "core", "werewolves.moon_bound_werewolves", BOOL));

        String vt = "vampirismTensura";
        all.add(SettingEntry.of(VAMPIRISM_TENSURA_FILE, vt, "cure_race", TEXT));
        all.add(SettingEntry.of(VAMPIRISM_TENSURA_FILE, vt, "race_level_floor", LIST));
        all.add(SettingEntry.of(VAMPIRISM_TENSURA_FILE, vt, "werewolf_skill_grants", LIST));
        all.add(SettingEntry.of(VAMPIRISM_TENSURA_FILE, vt, "werewolf_beastfolk_enabled", BOOL));
        all.add(SettingEntry.of(VAMPIRISM_TENSURA_FILE, vt, "tensura_holy_vs_vampire", DOUBLE));

        all.add(SettingEntry.of(SUPERNATURAL_FILE, "supernatural", "rite_complete_signal", BOOL));
        all.add(SettingEntry.of(SUPERNATURAL_FILE, "supernatural", "vampire_feed_blood", INT));
        all.add(SettingEntry.of(SUPERNATURAL_FILE, "supernatural", "tweak_sunlight_ignites_vampires", BOOL));
        all.add(SettingEntry.of(SUPERNATURAL_FILE, "supernatural", "tweak_cleansing_altar_disabled", BOOL).withClientCopy());
        all.add(SettingEntry.of(SUPERNATURAL_FILE, "supernatural", "tweak_hostile_mobs_attack_vampires", BOOL));
        all.add(SettingEntry.of(SUPERNATURAL_FILE, "supernatural", "tweak_skeletons_attack_werewolves", BOOL));

        all.add(SettingEntry.of(HUD_FILE, "hud", "rate_limit_per_second", INT));
        all.add(SettingEntry.of(HUD_FILE, "hud", "bite_reach", DOUBLE));
        all.add(SettingEntry.of(HUD_FILE, "hud", "wheel_enabled", BOOL));
        all.add(SettingEntry.of(HUD_FILE, "hud", "wheel_catalogue_interval_ticks", INT));
        all.add(SettingEntry.of(HUD_FILE, "hud", "wheel_unslotted_tensura", BOOL));
        all.add(SettingEntry.of(HUD_FILE, "hud", "mixins.next_preset_active", BOOL));
        all.add(SettingEntry.of(HUD_FILE, "hud", "mixins.tensura_feed_fills_blood", BOOL));
        all.add(SettingEntry.of(HUD_FILE, "hud", "mixins.hide_in_controls_mixin", BOOL).hide(RESERVED));

        all.add(SettingEntry.of(ARTIFACTS_FILE, "artifacts", "artifacts.compass_doppelganger_range", INT));

        species(all, "max_originals_per_world", INT);
        species(all, "transition_ticks", INT);
        species(all, "vampire_blood_ticks", INT);
        species(all, "bite_vampire_cost", INT);
        species(all, "max_five", INT);
        species(all, "only_alphas_turn", BOOL);
        species(all, "max_stacked_sparks", INT);
        species(all, "duel_reconnect_seconds", INT);
        species(all, "left_pack_grace_minutes", INT);
        species(all, "absent_leader_days", INT);
        species(all, "opac_link_enabled", BOOL);
        species(all, "witch_bonus_enabled", BOOL);
        species(all, "siphoner_chance", DOUBLE);
        species(all, "original_line_death", BOOL);
        species(all, "original_visibility", CHOICE);
        species(all, "line_rites_enabled", BOOL);
        species(all, "npc_line_weights", LIST);
        species(all, "imprisonment_ring_max_radius", INT);
        species(all, "imprisonment_rite_seconds", INT);
        species(all, "latent_gene_chance", DOUBLE);
        species(all, "alpha_bite_death_chance", DOUBLE);
        species(all, "lunar_eclipse_chance", DOUBLE);
        species(all, "doppelganger_gene_chance", DOUBLE);
        species(all, "doppelganger_villager_chance", DOUBLE);
        species(all, "gene_visibility", CHOICE);
        species(all, "doppelganger_rite_taglock", CHOICE);
        species(all, "hybrid_transition_blood", CHOICE);
        species(all, "full_moon_sleep", CHOICE);
        species(all, "delayed_awakening_nights", INT);
        for (String key : List.of("npc_packs_enabled", "npc_pack_dawn_despawn", "npc_alpha_spark_enabled", "moonblood_surge_enabled", "wolf_spirits_enabled")) {
            species(all, key, BOOL);
        }
        for (String key : List.of("npc_recruit_enabled", "npc_hunt_trips", "npc_chest_sort")) {
            species(all, key, BOOL);
        }
        for (String key : List.of("max_npc_members_per_player", "max_npc_members_per_pack", "npc_hunt_period_minutes")) {
            species(all, key, INT);
        }
        for (String key : List.of("npc_weight_brother", "npc_weight_tensura", "npc_bonus_share")) {
            species(all, key, DOUBLE);
        }
        species(all, "bottle_blood_cooldown_seconds", INT);
        species(all, "bottle_blood_health_cost", DOUBLE);
        species(all, "bottle_blood_daily_limit", INT);
        species(all, "give_blood_cooldown_seconds", INT);
        species(all, "force_feed_health_limit", DOUBLE);
        species(all, "vampire_blood_buff_enabled", BOOL);
        species(all, "vampire_blood_regen_cooldown_seconds", INT);
        species(all, "rip_heart_health", DOUBLE);
        species(all, "rip_heart_cooldown_seconds", INT);
        species(all, "neck_snap_cooldown_seconds", INT);
        species(all, "neck_snap_grace_seconds", INT);
        species(all, "hybrid_leap_range", DOUBLE);
        species(all, "hybrid_leap_damage", DOUBLE);
        species(all, "hybrid_leap_weakness_ticks", INT);
        species(all, "hybrid_leap_cooldown_seconds", INT);
        species(all, "neck_snap_kill_health", DOUBLE);
        species(all, "neck_snap_stagger_ticks", INT);
        species(all, "neck_snap_vampire_seconds", INT);
        species(all, "neck_snap_augustine_seconds", INT);
        species(all, "neck_snap_hybrid_seconds", INT);
        species(all, "neck_snap_original_seconds", INT);
        species(all, "venom_sick_after_ticks", INT);
        species(all, "augustine_attack_bonus", DOUBLE);
        species(all, "augustine_extra_health", DOUBLE);
        species(all, "augustine_blood_from_vampire_factor", DOUBLE);
        species(all, "augustine_blood_from_other_factor", DOUBLE);
        species(all, "hybrid_pack_moon_control", BOOL);
        species(all, "alpha_trial_enabled", BOOL);
        species(all, "alpha_trial_min_level", INT);
        species(all, "alpha_trial_hp_factor", DOUBLE);
        for (String key : List.of("npc_pack_chance", "npc_lone_wolf_chance", "npc_pack_alpha_chance", "feral_attack_factor", "feral_speed_factor")) {
            species(all, key, DOUBLE);
        }
        for (String key : List.of("npc_pack_min_size", "npc_pack_max_size", "npc_max_packs_per_night", "weak_spark_grow_days", "weak_spark_max_held", "weak_spark_cooldown_days")) {
            species(all, key, INT);
        }

        String st = "speciesTensura";
        all.add(SettingEntry.of(SPECIES_TENSURA_FILE, st, "layers", LIST).withClientCopy());
        all.add(SettingEntry.of(SPECIES_TENSURA_FILE, st, "rank_skills", LIST));
        all.add(SettingEntry.of(SPECIES_TENSURA_FILE, st, "undead_treatment_enabled", BOOL).withClientCopy());
        all.add(SettingEntry.of(SPECIES_TENSURA_FILE, st, "apex_evolutions_enabled", BOOL).withClientCopy());
        all.add(SettingEntry.of(SPECIES_TENSURA_FILE, st, "evolution_progress_multiplier", DOUBLE));
        all.add(SettingEntry.of(SPECIES_TENSURA_FILE, st, "sire_subordinate_link_enabled", BOOL));
        all.add(SettingEntry.of(SPECIES_TENSURA_FILE, st, "tensura_subordinates_join", BOOL));
        all.add(SettingEntry.of(SPECIES_TENSURA_FILE, st, "max_tensura_members_per_pack", INT));
        all.add(SettingEntry.of(SPECIES_TENSURA_FILE, st, "sire_naming_enabled", BOOL));
        all.add(SettingEntry.of(SPECIES_TENSURA_FILE, st, "severance_clears_name", BOOL));
        all.add(SettingEntry.of(SPECIES_TENSURA_FILE, st, "resire_naming_reward", BOOL));

        String ce = "celestials";
        all.add(SettingEntry.of(CELESTIALS_FILE, ce, "blood_moon_bite_bonus", DOUBLE));
        all.add(SettingEntry.of(CELESTIALS_FILE, ce, "blood_moon_pack_chance_factor", DOUBLE));
        all.add(SettingEntry.of(CELESTIALS_FILE, ce, "blue_moon_rite_cost", DOUBLE));
        all.add(SettingEntry.of(CELESTIALS_FILE, ce, "super_moon_rite_cost", DOUBLE));
        all.add(SettingEntry.of(CELESTIALS_FILE, ce, "super_blue_skips_forced_shift", BOOL));
        String bo = "bosses";
        all.add(SettingEntry.of(BOSSES_FILE, bo, "boss_check_seconds", INT));
        all.add(SettingEntry.of(BOSSES_FILE, bo, "boss_bars_enabled", BOOL));
        all.add(SettingEntry.of(BOSSES_FILE, bo, "original_return_days", INT));
        all.add(SettingEntry.of(BOSSES_FILE, bo, "npc_original_stake_needs_open", BOOL));
        all.add(SettingEntry.of(BOSSES_FILE, bo, "boss_reward_on", CHOICE));
        for (net.ofatech.supernaturalbloodlines.core.boss.BossId boss : net.ofatech.supernaturalbloodlines.core.boss.BossId.values()) {
            all.add(SettingEntry.of(BOSSES_FILE, bo, boss.id() + "_enabled", BOOL));
        }
        all.add(SettingEntry.of(BOSSES_FILE, bo, "boss_health_factor", DOUBLE));
        all.add(SettingEntry.of(BOSSES_FILE, bo, "boss_damage_factor", DOUBLE));
        all.add(SettingEntry.of(BOSSES_FILE, bo, "mikael_min_rung", INT));
        all.add(SettingEntry.of(BOSSES_FILE, bo, "mikael_night_chance", DOUBLE));
        all.add(SettingEntry.of(BOSSES_FILE, bo, "mikael_stakes_originals", BOOL));
        all.add(SettingEntry.of(BOSSES_FILE, bo, "klaus_chance_per_full_moon", DOUBLE));
        all.add(SettingEntry.of(BOSSES_FILE, bo, "klaus_hunts_players", BOOL));
        all.add(SettingEntry.of(BOSSES_FILE, bo, "klaus_warning_seconds", INT));
        all.add(SettingEntry.of(BOSSES_FILE, bo, "elijah_chance_per_day", DOUBLE));
        all.add(SettingEntry.of(BOSSES_FILE, bo, "elijah_oath_days", INT));
        all.add(SettingEntry.of(BOSSES_FILE, bo, "elixir_heal_fraction", DOUBLE));
        all.add(SettingEntry.of(BOSSES_FILE, bo, "rebekah_delay_seconds", INT));
        all.add(SettingEntry.of(BOSSES_FILE, bo, "kol_min_mark", INT));
        all.add(SettingEntry.of(BOSSES_FILE, bo, "kol_cloak_minutes", INT));
        all.add(SettingEntry.of(BOSSES_FILE, bo, "kol_grimoire_sp", INT));
        all.add(SettingEntry.of(BOSSES_FILE, bo, "finn_chance_per_eclipse", DOUBLE));
        all.add(SettingEntry.of(BOSSES_FILE, bo, "finn_ritual_seconds", INT));
        all.add(SettingEntry.of(BOSSES_FILE, bo, "link_seconds", INT));
        all.add(SettingEntry.of(BOSSES_FILE, bo, "link_damage_share", DOUBLE));
        all.add(SettingEntry.of(BOSSES_FILE, bo, "deucalion_min_sparks", INT));
        all.add(SettingEntry.of(BOSSES_FILE, bo, "deucalion_sparks", INT));
        all.add(SettingEntry.of(BOSSES_FILE, bo, "deucalion_pack_alphas", INT));
        all.add(SettingEntry.of(BOSSES_FILE, bo, "deucalion_respawn", BOOL));
        all.add(SettingEntry.of(BOSSES_FILE, bo, "deucalion_respawn_days", INT));
        all.add(SettingEntry.of(BOSSES_FILE, bo, "peter_house_enabled", BOOL));
        all.add(SettingEntry.of(BOSSES_FILE, bo, "peter_house_chance", DOUBLE));
        all.add(SettingEntry.of(BOSSES_FILE, bo, "peter_feral_full_moon", BOOL));
        all.add(SettingEntry.of(BOSSES_FILE, bo, "peter_respawn", BOOL));
        all.add(SettingEntry.of(BOSSES_FILE, bo, "peter_respawn_days", INT));
        all.add(SettingEntry.of(BOSSES_FILE, bo, "beast_host_chance", DOUBLE));
        all.add(SettingEntry.of(BOSSES_FILE, bo, "beast_respects_ash", BOOL));
        all.add(SettingEntry.of(BOSSES_FILE, bo, "beast_returns_once", BOOL));
        all.add(SettingEntry.of(BOSSES_FILE, bo, "beast_spirit_seconds", INT));
        all.add(SettingEntry.of(BOSSES_FILE, bo, "beast_roar_range", INT));
        all.add(SettingEntry.of(BOSSES_FILE, bo, "beast_roar_seconds", INT));
        all.add(SettingEntry.of(BOSSES_FILE, bo, "beast_roar_cooldown_seconds", INT));
        ALL = List.copyOf(all);
    }

    private AdminEntries() {}

    private static void species(List<SettingEntry> all, String key, SettingKind kind) {
        all.add(SettingEntry.of(SPECIES_FILE, "species", "species." + key, kind));
    }

    /** A config key or a game rule by id; hidden rows are found too (the caller refuses them). */
    public static Optional<SettingEntry> find(String id) {
        return find(ALL, RULES, id);
    }

    public static Optional<SettingEntry> find(List<SettingEntry> keys, List<SettingEntry> rules, String id) {
        return java.util.stream.Stream.concat(keys.stream(), rules.stream()).filter(e -> e.id().equals(id)).findFirst();
    }
}
