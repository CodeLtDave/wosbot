package dev.frostguard.tasks.economy;

import java.awt.Color;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.regex.Pattern;

import dev.frostguard.vision.convert.GameTimeUtils;
import dev.frostguard.vision.convert.RegexNumberParser;
import dev.frostguard.vision.ocr.ResilientOcrExecutor;
import dev.frostguard.api.configs.ConfigurationKeyEnum;
import dev.frostguard.api.configs.TemplatesEnum;
import dev.frostguard.api.configs.TpDailyTaskEnum;
import dev.frostguard.api.domain.ImageSearchResultData;
import dev.frostguard.api.domain.PointData;
import dev.frostguard.api.domain.AreaData;
import dev.frostguard.api.domain.AccountDescriptor;
import dev.frostguard.api.domain.OcrSettingsData;
import dev.frostguard.engine.service.StaminaService;
import dev.frostguard.engine.schedule.DelayedTask;
import dev.frostguard.engine.schedule.LaunchPoint;
import dev.frostguard.engine.nav.SearchConfigConstants;
import dev.frostguard.engine.nav.SidebarDestination;

/**
 * Task responsible for claiming rewards from the Storehouse.
 * 
 * <p>
 * This task:
 * <ul>
 * <li>Navigates to Storehouse rewards through their Daily sidebar rows</li>
 * <li>Claims daily chest rewards (available every few hours)</li>
 * <li>Claims stamina rewards (available once per day at game reset)</li>
 * <li>Reads timers via OCR to determine next availability</li>
 * <li>Reschedules based on the nearest reward time</li>
 * </ul>
 * 
 * <p>
 * <b>Reward Types:</b>
 * <ul>
 * <li>Chest: General resources, multiple claims per day</li>
 * <li>Stamina: 120 base stamina + bonus from Agnes expert</li>
 * </ul>
 */
public class StorehouseChestRoutine extends DelayedTask {

    // ========== Online Rewards Navigation ==========
    private static final AreaData STOREHOUSE_BODY_AFTER_ONLINE_REWARDS = new AreaData(
            new PointData(270, 660), new PointData(500, 790));
    private static final int STOREHOUSE_SELECTION_SETTLE_MILLIS = 1_000;
    private static final int STOREHOUSE_DESELECTION_SETTLE_MILLIS = 800;
    private static final PointData STOREHOUSE_SCROLL_START = new PointData(1, 636);
    private static final PointData STOREHOUSE_SCROLL_END = new PointData(2, 636);

    // ========== Chest Reward Coordinates ==========
    private static final PointData CHEST_TIMER_TOP_LEFT = new PointData(266, 1100);
    private static final PointData CHEST_TIMER_BOTTOM_RIGHT = new PointData(450, 1145);

    // ========== Stamina Reward Coordinates ==========
    private static final PointData STAMINA_AMOUNT_TOP_LEFT = new PointData(330, 600);
    private static final PointData STAMINA_AMOUNT_BOTTOM_RIGHT = new PointData(440, 680);

    // ========== Fallback Timer OCR ==========
    private static final PointData FALLBACK_TIMER_TOP_LEFT = new PointData(285, 642);
    private static final PointData FALLBACK_TIMER_BOTTOM_RIGHT = new PointData(430, 666);

    // ========== Constants ==========
    private static final int TIMER_OCR_MAX_ATTEMPTS = 3;
    private static final int MAX_TIMER_SECONDS = 7200; // 2 hours
    private static final int FALLBACK_RESCHEDULE_MINUTES = 5;
    private static final int BASE_STOREHOUSE_STAMINA = 120;
    private static final int SCROLL_ATTEMPT_COUNT = 2;
    private static final int SCROLL_REPEAT_DELAY = 300;

    // ========== OCR Settings ==========
    private static final OcrSettingsData STAMINA_OCR_SETTINGS = OcrSettingsData.assembler()
            .setTextColor(new Color(248, 247, 234))
            .stripBackground(true)
            .charWhitelist("0123456789")
            .textLayout(OcrSettingsData.TextLayout.SINGLE_LINE)

            .build();

    // ========== Configuration (loaded in loadConfiguration()) ==========
    private String storedStaminaTime;
    private ResilientOcrExecutor<LocalDateTime> textHelper;

    // ========== Execution State (reset each execution) ==========
    private LocalDateTime nextChestTime;
    private LocalDateTime nextStaminaTime;

    public StorehouseChestRoutine(AccountDescriptor profile, TpDailyTaskEnum tpDailyTask) {
        super(profile, tpDailyTask);
    }

    @Override
    protected boolean acceptsInjections() {
        return false;
    }

    /**
     * Loads task configuration from profile.
     */
    private void loadConfiguration() {
        // Check if we have a stored stamina claim time
        String storedStaminaTime = profile.getConfig(
                ConfigurationKeyEnum.STOREHOUSE_STAMINA_CLAIM_TIME_STRING, String.class);
        this.storedStaminaTime = storedStaminaTime;

        this.textHelper = new ResilientOcrExecutor<>(provider);

        logDebug(String.format("Configuration loaded - Stored stamina time: %s", storedStaminaTime));
    }

    /**
     * Resets execution-specific state.
     */
    private void resetExecutionState() {
        this.nextChestTime = null;
        this.nextStaminaTime = null;
        logDebug("Execution state reset");
    }

    @Override
    protected void execute() {
        loadConfiguration();
        resetExecutionState();

        ImageSearchResultData chest = openOnlineRewards();
        if (chest.isFound()) {
            processChestReward(chest);
        } else {
            logWarning("Online Rewards did not expose the Storehouse chest.");
            nextChestTime = LocalDateTime.now().plusMinutes(FALLBACK_RESCHEDULE_MINUTES);
        }

        if (isTimeToClaimStamina()) {
            processStaminaReward();
        }

        scheduleToNearestTime();

    }

    /**
     * Opens Online Rewards and removes the building-selection overlay so the chest bubble is visible.
     */
    private ImageSearchResultData openOnlineRewards() {
        logDebug("Opening Storehouse through the Online Rewards sidebar row");

        if (!navigationHelper.navigateToSidebarDestination(SidebarDestination.ONLINE_REWARDS)) {
            logWarning("Online Rewards sidebar destination was unavailable.");
            return ImageSearchResultData.miss();
        }

        // Online Rewards centers the camera at the Storehouse but initially points a tutorial hand
        // at its chest bubble. Selecting the building body and closing the selection removes the hand
        // without depending on the hand artwork or the previous city camera position.
        tapInside(STOREHOUSE_BODY_AFTER_ONLINE_REWARDS);
        sleepTask(STOREHOUSE_SELECTION_SETTLE_MILLIS);
        pressBack();
        sleepTask(STOREHOUSE_DESELECTION_SETTLE_MILLIS);

        ImageSearchResultData chest = searchForChest();
        logInfo(chest.isFound()
                ? "Online Rewards exposed the Storehouse chest after closing the building selection."
                : "Storehouse chest was not visible after closing the Online Rewards selection.");
        return chest;
    }

    /**
     * Processes the chest reward.
     * Searches for chest, claims it, and reads the next availability timer.
     */
    private void processChestReward(ImageSearchResultData chest) {
        logInfo("Searching for Storehouse chest reward.");

        if (chest.isFound()) {
            logInfo("Chest found. Claiming reward.");
            tapInside(chest);
            sleepTask(500); // Wait for reward screen

            nextChestTime = readChestTimer();

            if (nextChestTime == null) {
                nextChestTime = LocalDateTime.now().plusMinutes(FALLBACK_RESCHEDULE_MINUTES);
                logWarning("Failed to read chest timer, using fallback.");
            }

            // Close reward screen
            tapInside(STOREHOUSE_SCROLL_START, STOREHOUSE_SCROLL_END, SCROLL_ATTEMPT_COUNT, SCROLL_REPEAT_DELAY);
            return;
        }

        logWarning("Chest not found after maximum attempts. Trying fallback timer reading.");
        nextChestTime = readFallbackTimer();

        if (nextChestTime == null) {
            nextChestTime = LocalDateTime.now().plusMinutes(FALLBACK_RESCHEDULE_MINUTES);
        }
    }

    /**
     * Searches for chest templates with retries.
     */
    private ImageSearchResultData searchForChest() {
        ImageSearchResultData chest = templateSearchHelper.locatePattern(
                TemplatesEnum.STOREHOUSE_CHEST,
                SearchConfigConstants.SINGLE_WITH_RETRIES);

        if (chest.isFound()) {
            logDebug("Storehouse chest found");
            return chest;
        }

        ImageSearchResultData alternativeChest = templateSearchHelper.locatePattern(
                TemplatesEnum.STOREHOUSE_CHEST_2,
                SearchConfigConstants.SINGLE_WITH_RETRIES);
        if (alternativeChest.isFound()) {
            return alternativeChest;
        }

        return templateSearchHelper.locatePattern(
                TemplatesEnum.STOREHOUSE_CHEST_CURRENT,
                SearchConfigConstants.SINGLE_WITH_RETRIES);
    }

    /**
     * Reads the chest timer via OCR.
     */
    private LocalDateTime readChestTimer() {
        logDebug("Reading chest timer via OCR");

        OcrSettingsData configs = OcrSettingsData.assembler()
                .textLayout(OcrSettingsData.TextLayout.SINGLE_LINE)

                .stripBackground(true)
                .setTextColor(new Color(255, 95, 95))
                .charWhitelist("0123456789:")
                .build();

        LocalDateTime cooldown = textHelper.attemptRecognition(
                CHEST_TIMER_TOP_LEFT,
                CHEST_TIMER_BOTTOM_RIGHT,
                TIMER_OCR_MAX_ATTEMPTS,
                200L,
                configs,
                GameTimeUtils::isAcceptedFormat,
                text -> LocalDateTime.now().plus(GameTimeUtils.parseDuration(text)));

        if (cooldown == null) {
            logWarning("OCR returned empty time text");
            return null;
        }

        logDebug("Time OCR result: '" + GameTimeUtils.formatCountdown(cooldown) + "'");

        return cooldown;
    }

    /**
     * Checks if it's time to claim the stamina reward.
     * Stamina is claimed once per day at game reset.
     */
    private boolean isTimeToClaimStamina() {

        if (storedStaminaTime != null && !storedStaminaTime.isEmpty()) {
            try {
                LocalDateTime nextClaimTime = LocalDateTime.parse(storedStaminaTime);
                boolean timeToClaimAgain = LocalDateTime.now().isAfter(nextClaimTime);

                if (!timeToClaimAgain) {
                    logDebug("Stamina already claimed. Next claim at: " + nextClaimTime.format(DATETIME_FORMATTER));
                }

                nextStaminaTime = nextClaimTime;

                return timeToClaimAgain;
            } catch (Exception e) {
                logWarning("Failed to parse stored stamina claim time: " + e.getMessage());
            }
        }

        // First run or invalid stored time - allow claiming
        return true;
    }

    /**
     * Opens and claims the stamina reward from the A Warm Welcome sidebar row.
     */
    private void processStaminaReward() {
        logInfo("Opening Storehouse stamina reward through A Warm Welcome.");

        if (!navigationHelper.navigateToSidebarDestination(SidebarDestination.WARM_WELCOME)) {
            logWarning("A Warm Welcome sidebar destination was unavailable.");
            nextStaminaTime = LocalDateTime.now().plusMinutes(FALLBACK_RESCHEDULE_MINUTES);
            persistNextStaminaTime();
            return;
        }

        ImageSearchResultData claim = templateSearchHelper.locatePattern(
                TemplatesEnum.STOREHOUSE_WARM_WELCOME_CLAIM,
                SearchConfigConstants.SINGLE_WITH_RETRIES);
        if (!claim.isFound()) {
            logWarning("A Warm Welcome opened without a verified Claim control.");
            nextStaminaTime = LocalDateTime.now().plusMinutes(FALLBACK_RESCHEDULE_MINUTES);
            persistNextStaminaTime();
            return;
        }

        Integer displayedStamina = integerHelper.attemptRecognition(
                STAMINA_AMOUNT_TOP_LEFT,
                STAMINA_AMOUNT_BOTTOM_RIGHT,
                TIMER_OCR_MAX_ATTEMPTS,
                200L,
                STAMINA_OCR_SETTINGS,
                text -> RegexNumberParser.conformsTo(text, Pattern.compile(".*?(\\d+).*")),
                text -> RegexNumberParser.extractByPattern(text, Pattern.compile(".*?(\\d+).*")));

        int claimedStamina = displayedStamina != null && displayedStamina >= BASE_STOREHOUSE_STAMINA
                ? displayedStamina
                : BASE_STOREHOUSE_STAMINA;
        logDebug("A Warm Welcome stamina OCR result: "
                + (displayedStamina != null ? displayedStamina : "null; using base amount"));

        tapInside(claim);
        sleepTask(4_000);

        ImageSearchResultData remainingClaim = templateSearchHelper.locatePattern(
                TemplatesEnum.STOREHOUSE_WARM_WELCOME_CLAIM,
                SearchConfigConstants.DEFAULT_SINGLE);
        if (remainingClaim.isFound()) {
            logWarning("A Warm Welcome Claim control remained visible after the tap.");
            nextStaminaTime = LocalDateTime.now().plusMinutes(FALLBACK_RESCHEDULE_MINUTES);
            persistNextStaminaTime();
            return;
        }

        StaminaService.getServices().addExternalStamina(profile.getId(), claimedStamina);
        logInfo("Claimed " + claimedStamina + " stamina through A Warm Welcome.");
        nextStaminaTime = GameTimeUtils.nextCycleReset();
        persistNextStaminaTime();
    }

    private void persistNextStaminaTime() {
        writeProfileSetting(
                ConfigurationKeyEnum.STOREHOUSE_STAMINA_CLAIM_TIME_STRING,
                nextStaminaTime.toString());
    }

    /**
     * Reads timer using fallback OCR region.
     * Used when chest is not found but UI is still visible.
     */
    private LocalDateTime readFallbackTimer() {
        logDebug("Attempting fallback timer reading.");

        OcrSettingsData configs = OcrSettingsData.assembler()
                .textLayout(OcrSettingsData.TextLayout.SINGLE_LINE)

                .stripBackground(true)
                .setTextColor(new Color(255, 255, 255))
                .charWhitelist("0123456789:")
                .build();

        LocalDateTime cooldown = textHelper.attemptRecognition(
                FALLBACK_TIMER_TOP_LEFT,
                FALLBACK_TIMER_BOTTOM_RIGHT,
                TIMER_OCR_MAX_ATTEMPTS,
                200L,
                configs,
                GameTimeUtils::isAcceptedFormat,
                text -> LocalDateTime.now().plus(GameTimeUtils.parseDuration(text)));

        if (cooldown == null) {
            logWarning("OCR returned empty time text");
            return null;
        }

        logDebug("Time OCR result: '" + GameTimeUtils.formatCountdown(cooldown) + "'");

        // Validate timer is reasonable
        long secondsDiff = Duration.between(LocalDateTime.now(), cooldown).getSeconds();

        if (secondsDiff > MAX_TIMER_SECONDS) {
            logWarning(String.format("Timer exceeds 2 hours (%d min), using 1 hour fallback.", secondsDiff / 60));
            return LocalDateTime.now().plusHours(1);
        }

        return cooldown;
    }

    /**
     * Schedules the task to the nearest reward time.
     * Chest claims are checked more frequently than stamina (once per reset).
     */
    private void scheduleToNearestTime() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime nextReset = GameTimeUtils.dailyResetTime();

        // Validate chest time
        if (nextChestTime != null && nextChestTime.isBefore(now)) {
            logDebug("Chest time is in the past, treating as invalid.");
            nextChestTime = null;
        }

        // Cap chest time at reset to avoid missing stamina
        if (nextChestTime != null && nextChestTime.isAfter(nextReset)) {
            logInfo("Chest time exceeds reset, capping at reset time.");
            nextChestTime = nextReset;
        }

        // Validate stamina time
        if (nextStaminaTime != null && nextStaminaTime.isBefore(now)) {
            logDebug("Stamina time is in the past, treating as invalid.");
            nextStaminaTime = null;
        }

        // Determine which time is nearest and valid
        LocalDateTime scheduledTime;
        String reason;

        if (nextChestTime == null && nextStaminaTime == null) {
            scheduledTime = LocalDateTime.now().plusMinutes(FALLBACK_RESCHEDULE_MINUTES);
            reason = "No valid times (fallback)";
        } else if (nextChestTime == null) {
            scheduledTime = nextStaminaTime;
            reason = "stamina claim";
        } else if (nextStaminaTime == null) {
            scheduledTime = nextChestTime;
            reason = "chest claim";
        } else {
            // Both times valid - pick nearest
            if (nextChestTime.isBefore(nextStaminaTime)) {
                scheduledTime = nextChestTime;
                reason = "chest claim (nearest)";
            } else {
                scheduledTime = nextStaminaTime;
                reason = "stamina claim (nearest)";
            }
        }

        logInfo(String.format("Rescheduling for %s at: %s",
                reason, scheduledTime.format(DATETIME_FORMATTER)));

        if (!reason.contains("fallback")) {
            logDebug(String.format("Chest: %s, Stamina: %s",
                    (nextChestTime != null) ? nextChestTime.format(DATETIME_FORMATTER) : "null",
                    (nextStaminaTime != null) ? nextStaminaTime.format(DATETIME_FORMATTER) : "null"));
        }

        reschedule(scheduledTime);
    }

    @Override
    protected LaunchPoint getRequiredStartLocation() {
        return LaunchPoint.HOME;
    }

    @Override
    public boolean provideDailyMissionProgress() {
        return true;
    }
}
