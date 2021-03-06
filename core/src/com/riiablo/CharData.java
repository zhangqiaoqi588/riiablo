package com.riiablo;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.IntIntMap;
import com.riiablo.codec.D2S;
import com.riiablo.codec.excel.CharStats;
import com.riiablo.codec.excel.DifficultyLevels;
import com.riiablo.codec.excel.SetItems;
import com.riiablo.item.Attributes;
import com.riiablo.item.BodyLoc;
import com.riiablo.item.Item;
import com.riiablo.item.PropertyList;
import com.riiablo.item.Quality;
import com.riiablo.item.Stat;
import com.riiablo.item.StoreLoc;
import com.riiablo.item.Type;

import org.apache.commons.lang3.ArrayUtils;

import java.util.Arrays;
import java.util.EnumMap;

import static com.riiablo.item.StoreLoc.INVENTORY;

public class CharData {
  private D2S d2s;
  private CharacterClass charClass;
  private Item cursor;
  private final EnumMap<StoreLoc, Array<Item>> store = new EnumMap<>(StoreLoc.class);
  private final EnumMap<BodyLoc, Item> equipped = new EnumMap<>(BodyLoc.class);
  private final Array<Item> belt = new Array<>(16);
  private final Array<EquippedListener> EQUIPPED_LISTENERS = new Array<>();
  private final Array<SkillsListener> SKILLS_LISTENERS = new Array<>();

  private final IntIntMap equippedSets = new IntIntMap(); // Indexed using set id
  private final IntIntMap setItemsOwned = new IntIntMap(); // Indexed using set item id
  private final IntIntMap skills = new IntIntMap();
  private final Attributes stats = new Attributes();

  public CharData() {
    for (StoreLoc storeLoc : StoreLoc.values()) store.put(storeLoc, new Array<Item>());
  }

  public D2S getD2S() {
    return d2s;
  }

  public CharData setD2S(D2S d2s) {
    if (this.d2s != d2s) {
      this.d2s = d2s;
      charClass = CharacterClass.get(d2s.header.charClass);
    }

    return this;
  }

  public CharData createD2S(String name, CharacterClass charClass) {
    D2S.Header header = new D2S.Header();
    header.alternate = D2S.PRIMARY;
    header.name = name;
    header.flags = D2S.FLAG_EXPANSION;
    header.charClass = (byte) charClass.id;
    header.level = 1;
    header.hotkeys = new int[D2S.NUM_HOTKEYS];
    Arrays.fill(header.hotkeys, D2S.HOTKEY_UNASSIGNED);
    header.actions = new int[D2S.NUM_ACTIONS][D2S.NUM_BUTTONS];
    for (int[] actions : header.actions) Arrays.fill(actions, 0);

    D2S.QuestData quests = new D2S.QuestData();
    D2S.WaypointData waypoints = new D2S.WaypointData();
    D2S.NPCData npcs = new D2S.NPCData();

    CharStats.Entry charStats = charClass.entry();
    D2S.StatData stats = new D2S.StatData();
    stats.strength = charStats.str;
    stats.energy = charStats._int;
    stats.dexterity = charStats.dex;
    stats.vitality = charStats.vit;
    stats.statpts = 0;
    stats.newskills = 0;
    stats.hitpoints = stats.maxhp = (charStats.vit + charStats.hpadd) << Riiablo.files.ItemStatCost.get(Stat.hitpoints).ValShift;
    stats.mana = stats.maxmana = charStats._int << Riiablo.files.ItemStatCost.get(Stat.mana).ValShift;
    stats.stamina = stats.maxstamina = charStats.stamina << Riiablo.files.ItemStatCost.get(Stat.stamina).ValShift;
    stats.level = 1;
    stats.experience = 0;
    stats.gold = 0;
    stats.goldbank = 0;

    D2S.SkillData skills = new D2S.SkillData();
    skills.data = new byte[D2S.SkillData.NUM_TREES * D2S.SkillData.NUM_SKILLS];

    D2S.ItemData items = new D2S.ItemData();
    items.items = new Array<>(10);
    for (int i = 0; i < charStats.item.length; i++) {
      String code = charStats.item[i];
      if (code.isEmpty()) break;
      // TODO: generate item
    }

    header.merc = new D2S.MercData();
    header.merc.seed = 0;
    header.merc.name = 0;
    header.merc.type = 0;
    header.merc.flags = 0;
    header.merc.xp = 0;
    header.merc.items = new D2S.MercData.MercItemData();
    header.merc.items.items = new D2S.ItemData();
    header.merc.items.items.items = new Array<>();

    D2S.GolemData golem = new D2S.GolemData();

    D2S d2s = new D2S(null, header);
    d2s.quests = quests;
    d2s.waypoints = waypoints;
    d2s.npcs = npcs;
    d2s.stats = stats;
    d2s.skills = skills;
    d2s.items = items;
    d2s.golem = golem;

    return setD2S(d2s);
  }

  public void updateD2S(int difficulty) {
    DifficultyLevels.Entry diff = Riiablo.files.DifficultyLevels.get(difficulty);
    PropertyList base = stats.base();
    base.clear();
    base.put(Stat.strength, d2s.stats.strength);
    base.put(Stat.energy, d2s.stats.energy);
    base.put(Stat.dexterity, d2s.stats.dexterity);
    base.put(Stat.vitality, d2s.stats.vitality);
    base.put(Stat.statpts, d2s.stats.statpts);
    base.put(Stat.newskills, d2s.stats.newskills);
    base.put(Stat.hitpoints, d2s.stats.hitpoints);
    base.put(Stat.maxhp, d2s.stats.maxhp);
    base.put(Stat.mana, d2s.stats.mana);
    base.put(Stat.maxmana, d2s.stats.maxmana);
    base.put(Stat.stamina, d2s.stats.stamina);
    base.put(Stat.maxstamina, d2s.stats.maxstamina);
    base.put(Stat.level, d2s.stats.level);
    base.put(Stat.experience, (int) d2s.stats.experience);
    base.put(Stat.gold, d2s.stats.gold);
    base.put(Stat.goldbank, d2s.stats.goldbank);
    base.put(Stat.armorclass, 0);
    base.put(Stat.damageresist, 0);
    base.put(Stat.magicresist, 0);
    base.put(Stat.fireresist, diff.ResistPenalty);
    base.put(Stat.lightresist, diff.ResistPenalty);
    base.put(Stat.coldresist, diff.ResistPenalty);
    base.put(Stat.poisonresist, diff.ResistPenalty);
    base.put(Stat.maxfireresist, 75);
    base.put(Stat.maxlightresist, 75);
    base.put(Stat.maxcoldresist, 75);
    base.put(Stat.maxpoisonresist, 75);
    stats.reset();
    stats.update(this); // TODO: this need to be done whenever an item is changed

    skills.clear();
    for (int spellId = charClass.firstSpell, i = 0; spellId < charClass.lastSpell; spellId++, i++) {
      skills.put(spellId, d2s.skills.data[i]);
    }
    notifySkillsChanged(skills);
  }

  public CharacterClass getCharacterClass() {
    return charClass;
  }

  public void loadItems() {
    for (Array<Item> array : store.values()) array.clear();
    equipped.clear();
    belt.clear();
    cursor = null;
    stats.reset();
    for (Item item : d2s.items.items) {
      addItem(item); // A lot of this code is redundant
      //item.load();
    }
    stats.update(this);
    updateStats();
  }

  private void updateStats() {
    stats.reset();
    final int alternate = getAlternate();
    for (Item item : equipped.values()) {
      item.update();
      if (item.bodyLoc == BodyLoc.getAlternate(item.bodyLoc, alternate)) {
        stats.add(item.props.remaining());
        Stat stat;
        if ((stat = item.props.get(Stat.armorclass)) != null) {
          stats.aggregate().addCopy(stat);
        }
      }
    }
    for (Item item : store.get(StoreLoc.INVENTORY)) {
      if (item.type.is(Type.CHAR)) {
        stats.add(item.props.remaining());
      }
    }
    stats.update(this);

    // This appears to be hard-coded in the original client
    int dex = stats.get(Stat.dexterity).value();
    stats.aggregate().get(Stat.armorclass).add(dex / 4);

    skills.clear();
    for (int spellId = charClass.firstSpell, i = 0; spellId < charClass.lastSpell; spellId++, i++) {
      skills.put(spellId, d2s.skills.data[i]);
    }

    for (Stat stat : stats.remaining()) {
      if (stat.id == Stat.item_nonclassskill) {
        skills.getAndIncrement(stat.param, 0, stat.value());
      }
    }
    notifySkillsChanged(skills);
  }

  private void addItem(Item item) {
    switch (item.location) {
      case BELT:
        item.gridY = (byte) -(item.gridX >>> 2);
        item.gridX &= 0x3;
        belt.add(item);
        break;
      case CURSOR:
        assert cursor == null : "Only one item should be marked as cursor";
        cursor = item;
        break;
      case EQUIPPED:
        setEquipped(item.bodyLoc, item);
        item.update();
        if (item.bodyLoc == BodyLoc.getAlternate(item.bodyLoc, getAlternate())) {
          stats.add(item.props.remaining());
          Stat stat;
          if ((stat = item.props.get(Stat.armorclass)) != null) {
            stats.aggregate().addCopy(stat);
          }
        }
        break;
      case STORED:
        store.get(item.storeLoc).add(item);
        item.update();
        if (item.storeLoc == INVENTORY && item.type.is(Type.CHAR)) {
          stats.add(item.props.remaining());
        }
        break;
    }
    if (item.quality == Quality.SET) {
      setItemsOwned.getAndIncrement(item.qualityId, 0, 1);
    }
  }

  private void removeItem(Item item) {
    // TODO: e.g., item dropped
    if (item.quality == Quality.SET) {
      setItemsOwned.getAndIncrement(item.qualityId, 0, -1);
    }
  }

  private void updateSets(Item oldItem, Item item) {
    if (oldItem != null && oldItem.quality == Quality.SET) {
      SetItems.Entry setItem = (SetItems.Entry) oldItem.qualityData;
      int id = Riiablo.files.Sets.index(setItem.set);
      equippedSets.getAndIncrement(id, 0, -1);
    }
    if (item != null && item.quality == Quality.SET) {
      SetItems.Entry setItem = (SetItems.Entry) item.qualityData;
      int id = Riiablo.files.Sets.index(setItem.set);
      equippedSets.getAndIncrement(id, 0, 1);
    }
  }

  public int getSkill(int button) {
    return getSkill(d2s.header.alternate, button);
  }

  public int getSkill(int alternate, int button) {
    return d2s.header.actions[alternate][button];
  }

  public int getHotkey(int button, int skill) {
    return ArrayUtils.indexOf(d2s.header.hotkeys, button == Input.Buttons.LEFT
        ? skill | D2S.HOTKEY_LEFT_MASK
        : skill);
  }

  public Item getCursor() {
    return cursor;
  }

  public Item setCursor(Item item) {
    Item oldItem = cursor;
    this.cursor = item;
    return oldItem;
  }

  public Array<Item> getStore(StoreLoc storeLoc) {
    return store.get(storeLoc);
  }

  public Item getEquipped(BodyLoc bodyLoc) {
    return equipped.get(bodyLoc);
  }

  public Item getEquipped2(BodyLoc bodyLoc) {
    return getEquipped(BodyLoc.getAlternate(bodyLoc, d2s.header.alternate));
  }

  public Item setEquipped(BodyLoc bodyLoc, Item item) {
    Item oldItem = equipped.put(bodyLoc, item);
    if (item != null) item.update();
    updateSets(oldItem, item);
    updateStats();
    notifyEquippedChanged(bodyLoc, oldItem, item);
    return oldItem;
  }

  public Array<Item> getBelt() {
    return belt;
  }

  public int getAlternate() {
    return d2s.header.alternate;
  }

  public void setAlternate(int alternate) {
    if (d2s.header.alternate != alternate) {
      d2s.header.alternate = alternate;
      Item LH = getEquipped(alternate > 0 ? BodyLoc.LARM2 : BodyLoc.LARM);
      Item RH = getEquipped(alternate > 0 ? BodyLoc.RARM2 : BodyLoc.RARM);
      updateStats();
      notifyEquippedAlternated(alternate, LH, RH);
    }
  }

  public int alternate() {
    int alternate = getAlternate() > 0 ? 0 : 1;
    setAlternate(alternate);
    return alternate;
  }

  public IntIntMap getSets() {
    return equippedSets;
  }

  public IntIntMap getSetItems() {
    return setItemsOwned;
  }

  public IntIntMap getSkills() {
    return skills;
  }

  public Attributes getStats() {
    return stats;
  }

  private void notifyEquippedChanged(BodyLoc bodyLoc, Item oldItem, Item item) {
    for (EquippedListener l : EQUIPPED_LISTENERS) l.onChanged(this, bodyLoc, oldItem, item);
  }

  private void notifyEquippedAlternated(int alternate, Item LH, Item RH) {
    for (EquippedListener l : EQUIPPED_LISTENERS) l.onAlternated(this, alternate, LH, RH);
  }

  public boolean addEquippedListener(EquippedListener l) {
    EQUIPPED_LISTENERS.add(l);
    return true;
  }

  public boolean containsEquippedListener(EquippedListener l) {
    return l != null && EQUIPPED_LISTENERS.contains(l, true);
  }

  public boolean removeEquippedListener(EquippedListener l) {
    return l != null && EQUIPPED_LISTENERS.removeValue(l, true);
  }

  public boolean clearEquippedListeners() {
    boolean empty = EQUIPPED_LISTENERS.isEmpty();
    EQUIPPED_LISTENERS.clear();
    return !empty;
  }

  public interface EquippedListener {
    void onChanged(CharData client, BodyLoc bodyLoc, Item oldItem, Item item);
    void onAlternated(CharData client, int alternate, Item LH, Item RH);
  }

  public static class EquippedAdapter implements EquippedListener {
    @Override public void onChanged(CharData client, BodyLoc bodyLoc, Item oldItem, Item item) {}
    @Override public void onAlternated(CharData client, int alternate, Item LH, Item RH) {}
  }

  private void notifySkillsChanged(IntIntMap skills) {
    for (SkillsListener l : SKILLS_LISTENERS) l.onChanged(this, skills);
  }

  public boolean addSkillsListener(SkillsListener l) {
    SKILLS_LISTENERS.add(l);
    l.onChanged(this, skills);
    return true;
  }

  public interface SkillsListener {
    void onChanged(CharData client, IntIntMap skills);
  }
}
