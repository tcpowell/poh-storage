package com.pohstorage;

import lombok.Getter;
import lombok.Setter;
import net.runelite.api.widgets.Widget;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class PohStorageSet implements Comparable<PohStorageSet> {

    private PohStorageWidget outline;
    private PohStorageWidget arrow;
    private PohStorageWidget footer;
    private PohStorageWidget header;
    private List<PohStorageWidget> items;
    private String name;
    private int type;
    private boolean collapsible;
    private int column = 1;

    public PohStorageSet(PohStorageWidget outline, PohStorageWidget arrow, PohStorageWidget footer, PohStorageWidget header, List<PohStorageWidget> items, String name, int type, boolean collapsible) {
        this.outline = outline;
        this.arrow = arrow;
        this.footer = footer;
        this.header = header;
        this.items = items;
        this.name = name;
        this.type = type;
        this.collapsible = collapsible;
    }

    public List<PohStorageWidget> getAll() {
        List<PohStorageWidget> returnList = new ArrayList<PohStorageWidget>();
        returnList.add(outline);
        returnList.add(arrow);
        returnList.add(footer);
        returnList.add(header);
        returnList.addAll(items);
        return returnList;
    }

    public List<Widget> getAllWidgets() {
        List<Widget> returnList = new ArrayList<Widget>();
        for (PohStorageWidget poh : getAll()) {
            returnList.add(poh.getWidget());
        }
        return returnList;
    }

    public List<Widget> getWidgetItems() {
        List<Widget> returnList = new ArrayList<Widget>();
        for (PohStorageWidget poh : items) {
            returnList.add(poh.getWidget());
        }
        return returnList;
    }

    @Override
    public int compareTo(PohStorageSet o) {
        if (getName() == null || o.getName() == null) {
            return 0;
        }
        return getName().compareTo(o.getName());
    }
}
