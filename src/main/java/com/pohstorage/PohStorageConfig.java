package com.pohstorage;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.api.SpriteID;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Range;

@ConfigGroup("pohstorage")
public interface PohStorageConfig extends Config
{

	@Getter
	@RequiredArgsConstructor
	enum outlineColor
	{
		PURPLE("Purple", PohSprites.PURPLE.getSpriteId()),
		BLUE("Blue", PohSprites.BLUE.getSpriteId()),
		GREEN("Green", PohSprites.GREEN.getSpriteId()),
		YELLOW("Yellow", PohSprites.YELLOW.getSpriteId()),
		ORANGE("Orange", PohSprites.ORANGE.getSpriteId()),
		RED("Red", PohSprites.RED.getSpriteId()),
		NONE("None", 0);
		private final String value;
		private final int spriteId;
	}

	@Getter
	@RequiredArgsConstructor
	enum preserveFilters
	{
		NEVER("Never"),
		WITHIN_SESSION("Within Session"),
		ACROSS_SESSIONS("Across Sessions");
		private final String value;
	}

	@ConfigItem(
		keyName = "preserveFilters",
		name = "Save Filters",
		description = "Allows for saving prior filters each time a POH storage unit is opened",
		position = 1
	)
	default preserveFilters preserveFilters()
	{
		return preserveFilters.WITHIN_SESSION;
	}

	@ConfigItem(
		keyName = "fullSetColor",
		name = "Full Set Color",
		description = "Configures the shade color of full sets",
		position = 2
	)
	default outlineColor fullSetColor()
	{
		return outlineColor.GREEN;
	}

	@ConfigItem(
		keyName = "partialSetColor",
		name = "Partial Set Color",
		description = "Configures the shade color of partial sets",
		position = 3
	)
	default outlineColor partialSetColor()
	{
		return outlineColor.BLUE;
	}

	@ConfigItem(
		keyName = "emptySetColor",
		name = "Empty Set Color",
		description = "Configures the shade color of empty sets",
		position = 4
	)
	default outlineColor emptySetColor()
	{
		return outlineColor.NONE;
	}

	@ConfigItem(
		keyName = "shadeOpacity",
		name = "Shade Opacity",
		description = "Sets the opacity of shaded sets (0 = fully opaque, 255 = fully transparent)",
		position = 5
	)
	@Range(
		min = 0,
		max = 255
	)
	default int shadeOpacity()
	{
		return 230;
	}

	@ConfigItem(
		keyName = "showPartialSets",
		name = "Show Partial Sets",
		description = "Determines whether or not partial sets are displayed in POH storage units",
		hidden = true
	)
	default boolean showPartialSets()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showPartialSets",
		name = "Show Partial Sets",
		description = "Determines whether or not partial sets are displayed in POH storage units"
	)
	void showPartialSets(boolean show);

	@ConfigItem(
		keyName = "showEmptySets",
		name = "Show Empty Sets",
		description = "Determines whether or not empty sets are displayed in POH storage units",
		hidden = true
	)
	default boolean showEmptySets()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showEmptySets",
		name = "Show Empty Sets",
		description = "Determines whether or not empty sets are displayed in POH storage units"
	)
	void showEmptySets(boolean show);

	@ConfigItem(
		keyName = "showFullSets",
		name = "Show Full Sets",
		description = "Determines whether or not full sets are displayed in POH storage units",
		hidden = true
	)
	default boolean showFullSets()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showFullSets",
		name = "Show Full Sets",
		description = "Determines whether or not full sets are displayed in POH storage units"
	)
	void showFullSets(boolean show);

}
