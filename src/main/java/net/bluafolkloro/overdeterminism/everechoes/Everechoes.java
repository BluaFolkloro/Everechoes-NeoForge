package net.bluafolkloro.overdeterminism.everechoes;

import net.bluafolkloro.overdeterminism.everechoes.block.BirdFigureBlocks;
import net.bluafolkloro.overdeterminism.everechoes.block.ContainerBlocks;
import net.bluafolkloro.overdeterminism.everechoes.block.entity.ModBlockEntities;
import net.bluafolkloro.overdeterminism.everechoes.component.ModDataComponents;
import net.bluafolkloro.overdeterminism.everechoes.item.BirdFigureBlockItems;
import net.bluafolkloro.overdeterminism.everechoes.item.ContainerBlockItems;
import net.bluafolkloro.overdeterminism.everechoes.item.LetterItems;
import net.bluafolkloro.overdeterminism.everechoes.item.ModCreativeModeTabs;
import net.bluafolkloro.overdeterminism.everechoes.menu.ModMenuTypes;
import net.bluafolkloro.overdeterminism.everechoes.network.ModNetworking;
import net.bluafolkloro.overdeterminism.everechoes.postal.PostalExplorationTracker;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(Everechoes.MODID)
public class Everechoes {
    // Define mod id in a common place for everything to reference
    public static final String MODID = "everechoes";
    public Everechoes(IEventBus modEventBus) {
        //注册标签页
        ModCreativeModeTabs.register(modEventBus);

        //注册数据组件
        ModDataComponents.register(modEventBus);

        //注册方块实体
        ModBlockEntities.register(modEventBus);

        //注册菜单
        ModMenuTypes.register(modEventBus);

        //注册网络包
        modEventBus.addListener(ModNetworking::register);

        //记录玩家实际进入过的区块，供未来邮政地图册选择，不主动加载世界区块。
        NeoForge.EVENT_BUS.addListener(PostalExplorationTracker::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(PostalExplorationTracker::onPlayerLogout);

        //注册方块&物品
        LetterItems.register(modEventBus);
        ContainerBlocks.register(modEventBus);
        ContainerBlockItems.register(modEventBus);
        BirdFigureBlocks.register(modEventBus);
        BirdFigureBlockItems.register(modEventBus);
    }
}
