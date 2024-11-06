package vyrek.phasoritenetworks.ui.tabs

import io.wispforest.owo.ui.component.Components
import io.wispforest.owo.ui.container.FlowLayout
import io.wispforest.owo.ui.core.Sizing
import vyrek.phasoritenetworks.init.PNBlocks
import vyrek.phasoritenetworks.ui.UIScreen
import vyrek.phasoritenetworks.ui.flowLayout

class StatisticsTab(screen: UIScreen) : BaseTab(screen) {
	companion object {
		const val HEIGHT = 124
	}

	override fun build(root: FlowLayout) {
		val currentEntity = menu.player.level().getBlockEntity(menu.pos)!!
		val fakeImporter = PNBlocks.PHASORITE_IMPORTER.asItem().defaultInstance.also { i ->
			currentEntity.saveToItem(i, menu.player.level().registryAccess())
		}
		val fakeExporter = PNBlocks.PHASORITE_EXPORTER.asItem().defaultInstance.also { i ->
			currentEntity.saveToItem(i, menu.player.level().registryAccess())
		}

		root.flowLayout("flow-layout:container-importer").apply {
			child(Components.item(fakeImporter).sizing(Sizing.fill(), Sizing.fill()))
		}

		root.flowLayout("flow-layout:container-exporter").apply {
			child(Components.item(fakeExporter).sizing(Sizing.fill(), Sizing.fill()))
		}
	}
}