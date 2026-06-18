package net.bluafolkloro.overdeterminism.everechoes;

import net.bluafolkloro.overdeterminism.everechoes.menu.ModMenuTypes;
import net.bluafolkloro.overdeterminism.everechoes.screen.PostBoxScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(
        modid = Everechoes.MODID,
        value = Dist.CLIENT
)
public class EverechoesClient {

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenuTypes.POST_BOX_MENU.get(), PostBoxScreen::new);
    }
}
