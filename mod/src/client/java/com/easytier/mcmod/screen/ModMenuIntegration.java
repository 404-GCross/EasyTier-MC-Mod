package com.easytier.mcmod.screen;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

/**
 * ModMenu integration — adds "EasyTier Mod" to the Mods list with a gear icon
 * that opens the EasyTier configuration screen.
 */
public class ModMenuIntegration implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return EasyTierConfigScreen::new;
    }
}
