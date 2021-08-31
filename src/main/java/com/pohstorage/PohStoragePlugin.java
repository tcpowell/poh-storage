package com.pohstorage;

import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.WidgetClosed;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.widgets.*;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

import java.util.*;

@Slf4j
@PluginDescriptor(
		name = "POH Storage",
		description = "Filter and configure POH Storage",
		tags = {"poh","storage","filter"}
)
public class PohStoragePlugin extends Plugin
{
	private boolean showEmpty = true;
	private boolean showPartial = true;
	private boolean showFull = true;
	private Widget emptyCheck, partialCheck, fullCheck, emptyTitle, partialTitle, fullTitle;
	private int originalScroll;

	private static final String CONFIG_GROUP = "pohstorage";
	private List <PohStorageSet> storageSets = new ArrayList<PohStorageSet>();
	private List<PohStorageWidget> junkWidgets = new ArrayList<PohStorageWidget>();
	private List<Widget> addedDividers = new ArrayList<Widget>();
	private final int WIDGET_GROUP_ID = 675;
	private final int WIDGET_CHILD_TITLE = 2;
	private final int WIDGET_CHILD_SETS = 4;
	private final int WIDGET_SCROLL = 5;
	private final String TITLE_REGEX = "[\\(|:]";
	private final List<String> TITLES = Arrays.asList("Armour Case", "Cape Rack", "Toy Box", "Fancy Dress Box", "Treasure Chest", "Magic Wardrobe");

	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private PohStorageConfig config;

	@Override
	protected void startUp() throws Exception
	{
		log.debug("POH Storage Started!");
		loadConfig();
		if (pohStorageLoaded()) {
			clientThread.invokeLater(() ->
			{
				if (titleCheck()) {
					originalScroll = client.getWidget(WIDGET_GROUP_ID, WIDGET_CHILD_SETS).getScrollHeight();
					applyChanges();
					updateWidgetHeight(25);
					addControls();
				}
			});
		}
	}

	@Override
	protected void shutDown() throws Exception
	{
		log.debug("POH Storage Stopped!");
		if (pohStorageLoaded()) {
			clientThread.invokeLater(() ->
			{
				if (titleCheck()) {
					resetWidgets();
					removeControls();
					updateWidgetHeight(-25);
					emptyCheck = null;
					partialCheck = null;
					fullCheck = null;
					emptyTitle = null;
					partialTitle = null;
					fullTitle = null;
				}
			});
		}
	}

	@Subscribe
	public void onWidgetLoaded(WidgetLoaded e)
	{
		if (e.getGroupId() == WIDGET_GROUP_ID && pohStorageLoaded()) {
			clientThread.invokeLater(() ->
			{
				if (titleCheck()) {
					originalScroll = client.getWidget(WIDGET_GROUP_ID, WIDGET_CHILD_SETS).getScrollHeight();
					applyChanges();
					updateWidgetHeight(25);
					addControls();
				}
			});
		}
	}

	@Subscribe
	public void onWidgetClosed(WidgetClosed e)
	{
		if (e.getGroupId() == WIDGET_GROUP_ID && pohStorageLoaded()) {
			emptyCheck = null;
			partialCheck = null;
			fullCheck = null;
			emptyTitle = null;
			partialTitle = null;
			fullTitle = null;

			if (config.preserveFilters().getValue().equals("Never")) {
				showEmpty = true;
				showPartial = true;
				showFull = true;
			}
		}
	}

	@Subscribe
	public void onItemContainerChanged(ItemContainerChanged event)
	{
		if (pohStorageLoaded() && event.getContainerId() == InventoryID.INVENTORY.getId()) {
			clientThread.invokeLater(() ->
			{
				if (titleCheck()) {
					applyChanges();
				}
			});
		}
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged configChanged)
	{
		if (pohStorageLoaded() && configChanged.getGroup().equals(CONFIG_GROUP) && !configChanged.getKey().equals("showEmptySets") && !configChanged.getKey().equals("showPartialSets") && !configChanged.getKey().equals("showFullSets"))
			clientThread.invokeLater(() ->
			{
				if (titleCheck()) {
					applyChanges();
				}
			});
		else if (configChanged.getGroup().equals(CONFIG_GROUP) && configChanged.getKey().equals("preserveFilters") && configChanged.getNewValue().equals("Never")) {
			showEmpty = true;
			showPartial = true;
			showFull = true;
		}
	}

	@Provides
	PohStorageConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(PohStorageConfig.class);
	}

	public void loadConfig() {
		if (config.preserveFilters().getValue().equals("Across Sessions")) {
			showEmpty = config.showEmptySets();
			showPartial = config.showPartialSets();
			showFull = config.showFullSets();
		}
	}

	public void applyChanges() {
		resetWidgets();
		updateWidgetLists();
		adjustWidgets();
	}

	private boolean pohStorageLoaded() {
		return client.getWidget(WIDGET_GROUP_ID, WIDGET_CHILD_TITLE) != null && client.getWidget(WIDGET_GROUP_ID, WIDGET_CHILD_SETS) != null;
	}

	private boolean titleCheck() {
		return TITLES.contains(client.getWidget(WIDGET_GROUP_ID, WIDGET_CHILD_TITLE).getDynamicChildren()[1].getText().split(TITLE_REGEX)[0].trim());
	}

	private void updateWidgetHeight(int adjustment) {
		for (int x = 2; x <= 10; x++) {
			Widget temp = client.getWidget(WIDGET_GROUP_ID, x);
			if (x == 3)
				temp.setOriginalHeight(client.getWidget(WIDGET_GROUP_ID, x).getOriginalHeight() + adjustment);
			temp.revalidate();
			for (Widget tempChild : temp.getDynamicChildren()) {
				if (x != 4)
					tempChild.revalidate();
			}
		}
	}

	private void resetWidgets() {
		for (PohStorageSet storageSet : storageSets) {
			for (PohStorageWidget storageWidget : storageSet.getAll()) {
				storageWidget.getWidget().setOriginalX(storageWidget.getOriginalX());
				storageWidget.getWidget().setOriginalY(storageWidget.getOriginalY());
				storageWidget.getWidget().setHidden(storageWidget.isOriginalHidden());

				if (!storageWidget.isIcon()) {
					storageWidget.getWidget().setOpacity(storageWidget.getOriginalOpacity());
					storageWidget.getWidget().setSpriteId(storageWidget.getOriginalSpriteId());
					storageWidget.getWidget().setOriginalWidth(storageWidget.getOriginalWidth());
					storageWidget.getWidget().setWidthMode(storageWidget.getOriginalWidthMode());
					storageWidget.getWidget().setXPositionMode(storageWidget.getOriginalXPositionMode());
				}

				storageWidget.getWidget().revalidate();
			}
		}

		for (PohStorageWidget junk: junkWidgets) {
			junk.getWidget().setOriginalX(junk.getOriginalX());
			junk.getWidget().revalidate();
		}

		for (Widget addedDivider: addedDividers) {
			addedDivider.setHidden(true);
			addedDivider.revalidate();
		}

		Widget container = client.getWidget(WIDGET_GROUP_ID, WIDGET_CHILD_SETS);

		container.setScrollHeight(originalScroll);
		container.revalidateScroll();

		client.runScript(
				ScriptID.UPDATE_SCROLLBAR,
				WIDGET_GROUP_ID << 16 | WIDGET_SCROLL,
				WIDGET_GROUP_ID << 16 | WIDGET_CHILD_SETS,
				container.getScrollY()
		);
	}

	private void updateWidgetLists() {
		storageSets = new ArrayList<PohStorageSet>();
		junkWidgets = new ArrayList<PohStorageWidget>();
		addedDividers = new ArrayList<Widget>();

		for (Widget widgetItem : client.getWidget(WIDGET_GROUP_ID, WIDGET_CHILD_SETS).getDynamicChildren()) {
			if (widgetItem.getType()==4) {
				storageSets.add(newPohStorageSet(widgetItem));
			}
			else if (widgetItem.getWidth()==16) {
				junkWidgets.add(newPohStorageWidget(widgetItem));
				widgetItem.setOriginalX(-100);
				widgetItem.revalidate();
			}
		}
	}

	private void addControls() {
		if (emptyTitle == null || emptyCheck == null) {
			emptyTitle = addControlTitle(client.getWidget(WIDGET_GROUP_ID, WIDGET_CHILD_TITLE), 0, "Empty Sets");
			emptyCheck = addControlCheckbox(client.getWidget(WIDGET_GROUP_ID, WIDGET_CHILD_TITLE), 0, "Empty Sets", showEmpty);
		}
		else {
			emptyTitle.setHidden(false);
			emptyCheck.setHidden(false);
		}

		if (partialTitle == null || partialCheck == null) {
			partialTitle = addControlTitle(client.getWidget(WIDGET_GROUP_ID, WIDGET_CHILD_TITLE), 1, "Partial Sets");
			partialCheck = addControlCheckbox(client.getWidget(WIDGET_GROUP_ID, WIDGET_CHILD_TITLE), 1, "Partial Sets", showPartial);
		}
		else {
			partialTitle.setHidden(false);
			partialCheck.setHidden(false);
		}

		if (fullTitle == null || fullCheck == null) {
			fullTitle = addControlTitle(client.getWidget(WIDGET_GROUP_ID, WIDGET_CHILD_TITLE), 2, "Full Sets");
			fullCheck = addControlCheckbox(client.getWidget(WIDGET_GROUP_ID, WIDGET_CHILD_TITLE), 2, "Full Sets", showFull);
		}
		else {
			fullTitle.setHidden(false);
			fullCheck.setHidden(false);
		}
	}

	private void removeControls() {
		emptyTitle.setHidden(true);
		partialTitle.setHidden(true);
		fullTitle.setHidden(true);
		emptyCheck.setHidden(true);
		partialCheck.setHidden(true);
		fullCheck.setHidden(true);
	}

	private void toggle(Widget toggle, String tooltip, boolean show) {
		if (show) {
			toggle.setOnMouseOverListener((JavaScriptCallback) ev -> toggle.setSpriteId(SpriteID.SQUARE_CHECK_BOX_HOVERED));
			toggle.setOnMouseLeaveListener((JavaScriptCallback) ev -> toggle.setSpriteId(SpriteID.SQUARE_CHECK_BOX));
			toggle.setSpriteId(SpriteID.SQUARE_CHECK_BOX);
		}
		else {
			toggle.setOnMouseOverListener((JavaScriptCallback) ev -> toggle.setSpriteId(SpriteID.SQUARE_CHECK_BOX_CHECKED_HOVERED));
			toggle.setOnMouseLeaveListener((JavaScriptCallback) ev -> toggle.setSpriteId(SpriteID.SQUARE_CHECK_BOX_CHECKED));
			toggle.setSpriteId(SpriteID.SQUARE_CHECK_BOX_CHECKED);
		}

		if (tooltip.equals("Empty Sets")) {
			showEmpty = !showEmpty;
			config.showEmptySets(showEmpty);
		}
		else if (tooltip.equals("Partial Sets")) {
			showPartial = !showPartial;
			config.showPartialSets(showPartial);
		}
		else if (tooltip.equals("Full Sets")) {
			showFull = !showFull;
			config.showFullSets(showFull);
		}

		toggle.setOnOpListener((JavaScriptCallback) ev -> toggle(toggle, tooltip, !show));
		applyChanges();
	}

	private Widget addControlTitle(Widget parentWidget, int index, String title) {
		Widget titleWidget = parentWidget.createChild(200+index, 4);
		titleWidget.setText(title);
		titleWidget.setOriginalWidth(37);
		titleWidget.setOriginalHeight(15);
		titleWidget.setFontId(494);
		titleWidget.setTextColor(0xcfcfcf);
		titleWidget.setTextShadowed(true);
		titleWidget.setOriginalY(42);
		titleWidget.setOriginalX(index*175+31);
		titleWidget.revalidate();
		return titleWidget;
	}

	private Widget addControlCheckbox(Widget parentWidget, int index, String tooltip, boolean show) {
		Widget checkWidget = parentWidget.createChild(100+index, 5);
		checkWidget.setOriginalWidth(16);
		checkWidget.setOriginalHeight(16);
		checkWidget.setOriginalY(40);
		checkWidget.setOriginalX(index*175+11);
		checkWidget.setName(tooltip);
		checkWidget.setAction(1, "Toggle");

		checkWidget.setHasListener(true);
		checkWidget.setOnOpListener((JavaScriptCallback) ev -> toggle(checkWidget, tooltip, show));
		if (show) {
			checkWidget.setSpriteId(SpriteID.SQUARE_CHECK_BOX_CHECKED);
			checkWidget.setOnMouseOverListener((JavaScriptCallback) ev -> checkWidget.setSpriteId(SpriteID.SQUARE_CHECK_BOX_CHECKED_HOVERED));
			checkWidget.setOnMouseLeaveListener((JavaScriptCallback) ev -> checkWidget.setSpriteId(SpriteID.SQUARE_CHECK_BOX_CHECKED));
		}
		else {
			checkWidget.setSpriteId(SpriteID.SQUARE_CHECK_BOX);
			checkWidget.setOnMouseOverListener((JavaScriptCallback) ev -> checkWidget.setSpriteId(SpriteID.SQUARE_CHECK_BOX_HOVERED));
			checkWidget.setOnMouseLeaveListener((JavaScriptCallback) ev -> checkWidget.setSpriteId(SpriteID.SQUARE_CHECK_BOX));
		}

		checkWidget.revalidate();

		return checkWidget;
	}

	private void addDivider(int y) {
		Widget parent = client.getWidget(WIDGET_GROUP_ID, WIDGET_CHILD_SETS);
		int index = addedDividers.size() + 5000;
		Widget child = parent.createChild(index, 5);
		child.setOriginalWidth(16);
		child.setOriginalHeight(57);
		child.setOriginalX(194);
		child.setOriginalY(y);
		child.setSpriteId(SpriteID.UNKNOWN_BORDER_EDGE_VERTICAL);
		child.setSpriteTiling(true);
		child.revalidate();
		addedDividers.add(child);
	}

	private void adjustWidgets() {
		int normalOpacity = config.shadeOpacity();
		int hoverOpacity = (normalOpacity>20) ? normalOpacity - 20 : 0;
		int x=0;
		int top=0;

		List <PohStorageSet> hideSets = new ArrayList <PohStorageSet>();
		List <PohStorageSet> adjustSets = new ArrayList <PohStorageSet>();

		for (PohStorageSet storageSet : storageSets) {
			if((storageSet.getType()==0 && !showEmpty) || (storageSet.getType()==1 && !showPartial) || (storageSet.getType()==2 && !showFull))
				hideSets.add(storageSet);
			else
				adjustSets.add(storageSet);
		}

		Collections.sort(adjustSets);

		// Hide unwanted widgets
		for (PohStorageSet storageSet : hideSets) {
			for (Widget hideWidget : storageSet.getAllWidgets()) {
				hideWidget.setHidden(true);
			}
		}

		// Adjust others
		for (int i=0; i<adjustSets.size(); i++) {
			int finalI = i;

			if (i>0 && adjustSets.get(i-1).isCollapsible() && adjustSets.get(i-1).getColumn()==1 && adjustSets.get(i).isCollapsible()) {

				// Right Half
				adjustSets.get(i).setColumn(2);
				x--;

				adjustSets.get(i).getOutline().getWidget().setOriginalWidth(210);
				adjustSets.get(i).getOutline().getWidget().setWidthMode(1);
				adjustSets.get(i).getOutline().getWidget().setXPositionMode(2);

				adjustSets.get(i).getHeader().getWidget().setOriginalWidth(207);
				adjustSets.get(i).getHeader().getWidget().setXPositionMode(0);
				adjustSets.get(i).getHeader().getWidget().setWidthMode(0);
				adjustSets.get(i).getHeader().getWidget().setOriginalX(213);

				adjustSets.get(i).getFooter().getWidget().setOriginalWidth(210);
				adjustSets.get(i).getFooter().getWidget().setXPositionMode(2);
				adjustSets.get(i).getFooter().getWidget().setWidthMode(1);

				adjustSets.get(i).getArrow().getWidget().setOriginalX(3);

				top = x * 60;
				x++;

				shiftIcons(adjustSets.get(i).getWidgetItems(), 213, top+20);
				addDivider(top);

			}
			else if (i<adjustSets.size()-1 && adjustSets.get(i).isCollapsible() && adjustSets.get(i+1).isCollapsible()) {

				// Left Half
				adjustSets.get(i).getOutline().getWidget().setOriginalWidth(210);
				adjustSets.get(i).getOutline().getWidget().setWidthMode(0);
				adjustSets.get(i).getOutline().getWidget().setXPositionMode(0);

				adjustSets.get(i).getHeader().getWidget().setOriginalWidth(207);
				adjustSets.get(i).getHeader().getWidget().setXPositionMode(0);
				adjustSets.get(i).getHeader().getWidget().setWidthMode(0);
				adjustSets.get(i).getHeader().getWidget().setOriginalX(3);

				adjustSets.get(i).getFooter().getWidget().setOriginalWidth(210);
				adjustSets.get(i).getFooter().getWidget().setXPositionMode(0);
				adjustSets.get(i).getFooter().getWidget().setWidthMode(0);

				adjustSets.get(i).getArrow().getWidget().setOriginalX(216);

				top = x * 60;
				x++;

				shiftIcons(adjustSets.get(i).getWidgetItems(), 3, top+20);

			}
			else {

				// Full Width
				adjustSets.get(i).getOutline().getWidget().setOriginalWidth(0);
				adjustSets.get(i).getOutline().getWidget().setWidthMode(1);
				adjustSets.get(i).getOutline().getWidget().setXPositionMode(1);

				adjustSets.get(i).getHeader().getWidget().setOriginalWidth(3);
				adjustSets.get(i).getHeader().getWidget().setXPositionMode(2);
				adjustSets.get(i).getHeader().getWidget().setWidthMode(1);
				adjustSets.get(i).getHeader().getWidget().setOriginalX(0);

				adjustSets.get(i).getFooter().getWidget().setOriginalWidth(0);
				adjustSets.get(i).getFooter().getWidget().setXPositionMode(1);
				adjustSets.get(i).getFooter().getWidget().setWidthMode(1);

				adjustSets.get(i).getArrow().getWidget().setOriginalX(3);

				top = x * 60;
				x++;

				shiftIcons(adjustSets.get(i).getWidgetItems(), 3, top+20);

			}

			adjustSets.get(i).getOutline().getWidget().setOriginalY(top);

			if (adjustSets.get(i).getType()==0 && config.emptySetColor().getSpriteId() != 0) {
				adjustSets.get(i).getOutline().getWidget().setSpriteId(config.emptySetColor().getSpriteId());
				adjustSets.get(i).getOutline().getWidget().setOpacity(normalOpacity);
			}
			else if (adjustSets.get(i).getType()==1 && config.partialSetColor().getSpriteId() != 0) {
				adjustSets.get(i).getOutline().getWidget().setSpriteId(config.partialSetColor().getSpriteId());
				adjustSets.get(i).getOutline().getWidget().setOpacity(normalOpacity);

				adjustSets.get(i).getOutline().getWidget().setOnMouseRepeatListener((JavaScriptCallback) ev -> adjustSets.get(finalI).getOutline().getWidget().setOpacity(hoverOpacity));
				adjustSets.get(i).getOutline().getWidget().setOnMouseLeaveListener((JavaScriptCallback) ev -> adjustSets.get(finalI).getOutline().getWidget().setOpacity(normalOpacity));
			}
			else if (adjustSets.get(i).getType()==2 && config.fullSetColor().getSpriteId() != 0) {
				adjustSets.get(i).getOutline().getWidget().setSpriteId(config.fullSetColor().getSpriteId());
				adjustSets.get(i).getOutline().getWidget().setOpacity(normalOpacity);
				adjustSets.get(i).getOutline().getWidget().setOnMouseRepeatListener((JavaScriptCallback) ev -> adjustSets.get(finalI).getOutline().getWidget().setOpacity(hoverOpacity));
				adjustSets.get(i).getOutline().getWidget().setOnMouseLeaveListener((JavaScriptCallback) ev -> adjustSets.get(finalI).getOutline().getWidget().setOpacity(normalOpacity));
			}

			adjustSets.get(i).getOutline().getWidget().revalidate();

			adjustSets.get(i).getHeader().getWidget().setOriginalY(top);
			adjustSets.get(i).getHeader().getWidget().revalidate();

			adjustSets.get(i).getArrow().getWidget().setOriginalY(top+5);
			adjustSets.get(i).getArrow().getWidget().revalidate();

			adjustSets.get(i).getFooter().getWidget().setOriginalY(top+40);
			adjustSets.get(i).getFooter().getWidget().revalidate();
		}

		Widget container = client.getWidget(WIDGET_GROUP_ID, WIDGET_CHILD_SETS);

		int y = x*60-3;
		if (container.getHeight()>y)
			y=0;

		container.setScrollHeight(y);
		container.revalidateScroll();

		client.runScript(
				ScriptID.UPDATE_SCROLLBAR,
				WIDGET_GROUP_ID << 16 | WIDGET_SCROLL,
				WIDGET_GROUP_ID << 16 | WIDGET_CHILD_SETS,
				container.getScrollY()
		);
	}

	private void shiftIcons(List<Widget> items, int adjustmentX, int finalY) {
		for (int j=0; j<items.size(); j++) {
			items.get(j).setOriginalX(42*j + adjustmentX);
			items.get(j).setOriginalY(finalY);
			items.get(j).revalidate();
		}
	}

	private PohStorageWidget newPohStorageWidget(Widget widget) {
		return new PohStorageWidget(widget, widget.getId(), widget.getOriginalX(), widget.getOriginalY(), widget.getOriginalWidth(), widget.getOpacity(), widget.getSpriteId(), widget.isHidden(), widget.getWidthMode(), widget.getXPositionMode());
	}

	private PohStorageSet newPohStorageSet(Widget headerWidget) {
		int textId = headerWidget.getIndex();

		// Create building blocks
		PohStorageWidget outline = newPohStorageWidget(headerWidget.getParent().getChild(textId-3));
		PohStorageWidget arrow = newPohStorageWidget(headerWidget.getParent().getChild(textId-2));
		PohStorageWidget footer = newPohStorageWidget(headerWidget.getParent().getChild(textId-1));
		PohStorageWidget header = newPohStorageWidget(headerWidget);

		// Get item widgets
		int stored = 0;
		int offset = 1;
		List<PohStorageWidget> items = new ArrayList<PohStorageWidget>();
		while (headerWidget.getParent().getChild(textId+offset).getWidth()==36) {
			if (!headerWidget.getParent().getChild(textId+offset).isHidden()) {
				items.add(newPohStorageWidget(headerWidget.getParent().getChild(textId + offset)));
				if (headerWidget.getParent().getChild(textId + offset).getOpacity()==0)
					stored++;
			}
			offset++;
		}

		int type;
		if (stored == items.size())
			type = 2; // Full
		else if (stored == 0)
			type = 0; // Empty
		else
			type = 1; // Partial

		return new PohStorageSet(outline, arrow, footer, header, items, headerWidget.getText(), type, items.size()<=4);
	}

}
