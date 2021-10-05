package com.pohstorage;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.runelite.api.widgets.Widget;

@Getter
@Setter
@AllArgsConstructor
public class PohStorageWidget
{
	private Widget widget;
	private int id;
	private int originalX;
	private int originalY;
	private int originalWidth;
	private int originalOpacity;
	private int originalSpriteId;
	private boolean originalHidden;
	private int originalWidthMode;
	private int originalXPositionMode;

	public boolean isIcon()
	{
		return originalWidth == 36;
	}
}
