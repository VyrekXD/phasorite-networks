package vyrek.phasoritenetworks.client.ui.tabs

import io.wispforest.owo.ui.component.Components
import io.wispforest.owo.ui.container.FlowLayout
import io.wispforest.owo.ui.core.Color
import io.wispforest.owo.ui.core.Sizing
import io.wispforest.owo.ui.core.Surface
import net.minecraft.network.chat.Component
import vyrek.phasoritenetworks.client.render.Highlight
import vyrek.phasoritenetworks.client.ui.*
import vyrek.phasoritenetworks.common.networks.ComponentType
import vyrek.phasoritenetworks.common.searchBy
import vyrek.phasoritenetworks.init.PNBlocks
import vyrek.phasoritenetworks.networking.PNEndecsData

enum class ComponentSortMethod {
	All,
	Exporters,
	Importers
}

fun capitalizeSentence(input: String): String =
	input.split("_").joinToString(" ") { it.replaceFirstChar { char -> char.uppercaseChar() } }

class ComponentsTab(screen: UIScreen) :
	BaseScrollTab<ComponentSortMethod, PNEndecsData.RawComponentData>(screen) {
	companion object {
		const val EXPORTER_COLOR = 0xFFFE4A49
		const val IMPORTER_COLOR = 0xFF87B37A
		const val OUTLINE_COLOR = 0xFFEEE5E9
	}

	override val height = 177
	override var sortedBy = ComponentSortMethod.All
	override val scrollData = menu.network!!.components

	private val selectionBatch = mutableListOf<PNEndecsData.RawComponentData>()

	lateinit var selectionHandler: (Boolean) -> Unit

	override fun build(root: FlowLayout) {
		super.build(root)

		val cancel = root.button("button:selection-cancel")
		val disconnect = root.button("button:selection-disconnect")
		val containerButtons = root.flowLayout("flow-layout:container-buttons").apply {
			removeChild(cancel)
			removeChild(disconnect)
		}
		val emptyContainer = root.flowLayout("flow-layout:container-empty")
		selectionHandler = handler@{ removed ->
			if (removed) {
				containerButtons.clearChildren()
				emptyContainer.sizing(Sizing.content(), Sizing.content())

				return@handler
			}

			containerButtons.children(
				mutableListOf(
					cancel,
					disconnect
				)
			)
			emptyContainer.sizing(Sizing.content(), Sizing.fixed(16))
		}

		cancel.onPress {
			buildScroll()

			selectionBatch.clear()
			selectionHandler(true)
		}

		disconnect.onPress {
			menu.disconnectComponents(selectionBatch.map { it.globalPos })
			selectionBatch.clear()

			selectionHandler(true)
		}
	}

	override fun buildScrollData(i: Int, data: PNEndecsData.RawComponentData) {
		val color =
			if (data.type == ComponentType.EXPORTER) EXPORTER_COLOR else IMPORTER_COLOR

		scroll.child(
			screen.uiModel.expandTemplate(
				FlowLayout::class, "tab:components:data", mutableMapOf(
					"i" to "$i",
					"color" to Color.ofArgb(color.toInt()).asHexString(false),
					"name" to data.name,
					"energy" to parseLong(data.throughput),
					"rawEnergy" to formatStr(data.throughput)
				)
			).apply {
				mouseDown().subscribe { _, _, _ ->
					if (!canPlayerEdit()) return@subscribe true

					if (data in selectionBatch) {
						surface(Surface.outline(color.toInt()))
						selectionBatch.remove(data)
						if (selectionBatch.isEmpty()) selectionHandler(true)
					} else {
						surface(Surface.outline(OUTLINE_COLOR.toInt()))
						if (selectionBatch.isEmpty()) selectionHandler(false)
						selectionBatch.add(data)
					}

					return@subscribe true
				}
			}
		)

		val item = when (data.type) {
			ComponentType.IMPORTER -> PNBlocks.PHASORITE_IMPORTER.asItem().defaultInstance
			ComponentType.EXPORTER -> PNBlocks.PHASORITE_EXPORTER.asItem().defaultInstance
			else -> null
		}!!.also {
			menu.clientEntity.saveToItem(it, menu.player.level().registryAccess())
		}
		scroll.flowLayout("flow-layout:container-item-$i")
			.child(Components.item(item).sizing(Sizing.fill(), Sizing.fill()))
			.apply {
				mouseDown().subscribe { _, _, _ ->
					if (data.globalPos.dimension != menu.player.level().dimension()) return@subscribe true

					handleRender(data)
					return@subscribe true
				}

				val tooltipMessage = if (data.globalPos.dimension == menu.player.level().dimension()) {
					listOf(
						Component.literal("X: §7${data.globalPos.pos.x}§r | Y: §7${data.globalPos.pos.y}§r | Z: §7${data.globalPos.pos.z}§r"),
						Component.literal("§7Click to outline block")
					)
				} else {
					listOf(
						Component.literal("X: §7${data.globalPos.pos.x}§r | Y: §7${data.globalPos.pos.y}§r | Z: §7${data.globalPos.pos.z}§r"),
						Component.literal("Dimension: ${capitalizeSentence(data.globalPos.dimension.location().path)}")
					)
				}

				tooltip(tooltipMessage)
			}
	}

	private fun handleRender(component: PNEndecsData.RawComponentData) {
		val playerToComponent = component.globalPos.pos.distSqr(menu.player.onPos)
		val distanceMultiplier = playerToComponent.coerceIn(1.0, 20.0)

		Highlight.handler.add(
			component.globalPos.pos,
			System.currentTimeMillis() + (Highlight.BASE_DURATION_MS * distanceMultiplier).toLong(),
			menu.network!!.color
		)
	}

	override fun filterData(): List<PNEndecsData.RawComponentData> {
		val filteredComponent = when (sortedBy) {
			ComponentSortMethod.All -> scrollData
			ComponentSortMethod.Exporters -> scrollData.filter { it.type == ComponentType.EXPORTER }
			ComponentSortMethod.Importers -> scrollData.filter { it.type == ComponentType.IMPORTER }
		}

		return searchBy?.let { filteredComponent.searchBy(it) { data -> data.name } } ?: filteredComponent
	}
}