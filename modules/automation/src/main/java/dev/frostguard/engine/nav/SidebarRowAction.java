package dev.frostguard.engine.nav;

import dev.frostguard.api.configs.TemplatesEnum;

/** Visually verified controls that may belong to a detected sidebar row. */
public enum SidebarRowAction {
    GO(TemplatesEnum.SIDEBAR_GO_ACTION, TemplatesEnum.SIDEBAR_GO_ACTION_NOTIFICATION,
            TemplatesEnum.SIDEBAR_GO_ACTION_PROGRESS),
    CLAIM(TemplatesEnum.SIDEBAR_CLAIM_ACTION, TemplatesEnum.SIDEBAR_CLAIM_ACTION_REWARD);

    private final TemplatesEnum[] templates;

    SidebarRowAction(TemplatesEnum... templates) {
        this.templates = templates.clone();
    }

    public TemplatesEnum[] templates() {
        return templates.clone();
    }
}
