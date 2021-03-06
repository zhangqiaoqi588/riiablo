package com.riiablo.item;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Bits;
import com.riiablo.CharData;
import com.riiablo.Riiablo;
import com.riiablo.codec.excel.ItemStatCost;

public class Attributes {
  final PropertyList base = new PropertyList();
  final PropertyList agg = new PropertyList();
  final PropertyList rem = new PropertyList();
  final Array<PropertyList> propertyLists = new Array<>();
  final Bits mod = new Bits(1 << Stat.BITS);

  public PropertyList base() {
    return base;
  }

  public PropertyList aggregate() {
    return agg;
  }

  public PropertyList remaining() {
    return rem;
  }

  public boolean isModified(int stat) {
    return mod.get(stat);
  }

  public int size() {
    return agg.size();
  }

  public Stat get(int stat) {
    return agg.get(stat);
  }

  public void reset() {
    agg.clear();
    agg.deepCopy(base);
    mod.clear();
    rem.clear();
    propertyLists.clear();
  }

  public void add(PropertyList props) {
    propertyLists.add(props);
  }

  public void update(CharData charData) {
    for (PropertyList list : propertyLists) {
      if (list != null) update(charData, list);
    }
  }

  private void update(CharData charData, PropertyList list) {
    for (Stat stat : list) {
      if (stat.entry.op > 0) {
        boolean empty = op(charData, stat, stat.entry);
        if (empty) {
          rem.addCopy(stat);
        }
      } else if (base.get(stat.hash) == null) {
        rem.addCopy(stat);
      } else {
        agg.addCopy(stat);
        mod.set(stat.id);
      }
    }
  }

  private boolean op(CharData charData, Stat stat, ItemStatCost.Entry entry) {
    int op = entry.op;
    int op_base = entry.op_param > 0
        ? charData.getStats().get(Riiablo.files.ItemStatCost.index(entry.op_base)).val
        : 1;
    int op_param = entry.op_param;

    int opCount = 0;
    for (String op_stat : entry.op_stat) {
      if (op_stat.isEmpty()) break;
      int statId = Riiablo.files.ItemStatCost.index(op_stat);
      Stat opstat = agg.get(statId);
      if (opstat != null) {
        opstat.val += op(charData, stat, base.get(statId), op, op_base, op_param);
        mod.set(opstat.id);
        opCount++;
      }
    }
    return opCount == 0;
  }

  private int op(CharData charData, Stat stat, Stat opstat, int op, int op_base, int op_param) {
    switch (op) {
      case 1:  return (stat.val * opstat.val) / 100;
      case 2:  return (stat.val * op_base) / (1 << op_param);
      case 3:  return (stat.val * op_base) / (1 << op_param) * opstat.val / 100;
      case 4:  return (stat.val * op_base) / (1 << op_param);
      case 5:  return (stat.val * op_base) / (1 << op_param) * opstat.val / 100;
      case 6:  return 0; // by-time
      case 7:  return 0; // by-time percent
      case 8:
        agg.addCopy(stat);
        mod.set(stat.id);
        return stat.val * charData.getCharacterClass().entry().ManaPerMagic; // max mana
      case 9:
        if (opstat.id == Stat.maxhp) { // only increment vit on maxhp op
          agg.addCopy(stat);
          mod.set(stat.id);
        }
        return stat.val // max hitpoints
          * (opstat.id == Stat.maxhp
          ? charData.getCharacterClass().entry().LifePerVitality
          : charData.getCharacterClass().entry().StaminaPerVitality);
      case 10: return 0; // no-op
      case 11: return (stat.val * opstat.val) / 100; // TODO: modify field value? used with item_maxhp_percent and item_maxmana_percent
      case 12: return 0; // no-op
      case 13: return (stat.val * opstat.val) / 100;
      default: throw new AssertionError("Unsupported op: " + op + " for " + stat);
    }
  }
}
