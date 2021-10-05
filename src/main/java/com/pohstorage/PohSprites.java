package com.pohstorage;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.client.game.SpriteOverride;

@RequiredArgsConstructor
public enum PohSprites implements SpriteOverride
{
	PURPLE(-73100, "purple.png"),
	BLUE(-73101, "blue.png"),
	GREEN(-73102, "green.png"),
	YELLOW(-73103, "yellow.png"),
	ORANGE(-73104, "orange.png"),
	RED(-73105, "red.png");

	@Getter
	private final int spriteId;

	@Getter
	private final String fileName;
}
