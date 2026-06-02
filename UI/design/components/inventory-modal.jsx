// components/inventory-modal.jsx
// Inventory Modal (1280×800) + ItemTooltip (popup card, 4 rarity variants).
// libGDX: Stage modal layer with scrim · table(2 cols) for grid+detail · separate Tooltip popover.

const RARITY = {
  common:    { name: "普通", en: "COMMON",     color: "var(--muted-text)",   dim: "#5b6068" },
  uncommon:  { name: "精良", en: "UNCOMMON",   color: "var(--stamina-green)", dim: "var(--stamina-green-dim)" },
  rare:      { name: "稀有", en: "RARE",       color: "var(--cold-cyan)",     dim: "var(--cold-cyan-dim)" },
  epic:      { name: "史诗", en: "EPIC",       color: "var(--arcane-violet)", dim: "var(--arcane-violet-dim)" },
  artifact:  { name: "传奇", en: "ARTIFACT",   color: "var(--ember-gold)",    dim: "var(--ember-gold-dim)" },
};

const SAMPLE_ITEMS = [
  // row 0
  { id: "i01", name: "钉刺长剑 +1",   slot: "weapon",   tier: 2, rarity: "uncommon", token: "eq.sword_t2",   count: null, equipped: true,  stats: [{k:"攻击",v:"+18 物理"}, {k:"暴击",v:"+4%"}, {k:"破甲",v:"+2"}] },
  { id: "i02", name: "铁盾",          slot: "shield",   tier: 1, rarity: "common",   token: "eq.shield_t1",  count: null, equipped: true,  stats: [{k:"防御",v:"+8"},   {k:"格挡",v:"+12%"}] },
  { id: "i03", name: "矿工头盔",      slot: "helmet",   tier: 1, rarity: "common",   token: "eq.helm_t1",    count: null, equipped: true },
  { id: "i04", name: "皮革胸甲",      slot: "armor",    tier: 2, rarity: "uncommon", token: "eq.armor_t2",   count: null, equipped: true },
  { id: "i05", name: "暮光斗篷",      slot: "cloak",    tier: 2, rarity: "rare",     token: "eq.cloak_t2",   count: null },
  { id: "i06", name: "战手套",        slot: "gloves",   tier: 1, rarity: "common",   token: "eq.gloves_t1",  count: null },
  { id: "i07", name: "护符·虚空",     slot: "amulet",   tier: 3, rarity: "epic",     token: "eq.amulet_t3",  count: null },
  { id: "i08", name: "信誓之戒",      slot: "ring",     tier: 2, rarity: "rare",     token: "eq.ring_t2",    count: null, selected: true },
  // row 1
  { id: "i09", name: "矿工长靴",      slot: "boots",    tier: 1, rarity: "common",   token: "eq.boots_t1",   count: null },
  { id: "i10", name: "治疗药水",      slot: "potion",   tier: 1, rarity: "common",   token: "inv.potion_hp", count: 2 },
  { id: "i11", name: "魔力药水",      slot: "potion",   tier: 1, rarity: "uncommon", token: "inv.potion_mp", count: 3 },
  { id: "i12", name: "瞬移卷轴",      slot: "scroll",   tier: 2, rarity: "rare",     token: "inv.scroll",    count: 3 },
  { id: "i13", name: "黄铜钥匙",      slot: "key",      tier: 1, rarity: "common",   token: "inv.key",       count: 1 },
  { id: "i14", name: "金币",          slot: "currency", tier: 0, rarity: "common",   token: "inv.gold",      count: 215 },
  { id: "i15", name: "红宝石",        slot: "gem",      tier: 2, rarity: "rare",     token: "inv.ruby",      count: 5 },
  { id: "i16", name: "苦根草",        slot: "herb",     tier: 1, rarity: "common",   token: "inv.herb",      count: 2 },
  // row 2
  { id: "i17", name: "深渊水晶",      slot: "gem",      tier: 3, rarity: "epic",     token: "inv.crystal",   count: 1 },
  { id: "i18", name: "破损护腕",      slot: "gloves",   tier: 0, rarity: "common",   token: "eq.gloves_t0",  count: null, broken: true },
  { id: "i19", name: "古旧短剑",      slot: "weapon",   tier: 1, rarity: "common",   token: "eq.sword_t1",   count: null },
  { id: "i20", name: "矿镐",          slot: "tool",     tier: 1, rarity: "common",   token: "tool.pickaxe",  count: 1 },
  null, null, null, null,
  // empty rows
  null, null, null, null, null, null, null, null,
  null, null, null, null, null, null, null, null,
];

const FILTERS = [
  { k: "all",       label: "全部",     count: 20 },
  { k: "weapon",    label: "武器",     count: 2  },
  { k: "armor",     label: "护甲",     count: 5  },
  { k: "consumable",label: "消耗",     count: 5  },
  { k: "material",  label: "材料",     count: 5  },
  { k: "key",       label: "钥匙",     count: 2  },
];

function InventoryModal({ activeFilter = "all", selectedId = "i08", showOverlay = false, showTooltip = true }) {
  const selected = SAMPLE_ITEMS.find(i => i && i.id === selectedId) || SAMPLE_ITEMS.find(i => i);
  return (
    <div data-screen-label="inventory_modal" style={{
      width: 1280, height: 800,
      position: "relative",
      background: "radial-gradient(80% 60% at 50% 40%, rgba(28,40,48,0.6), rgba(5,7,10,0.95))",
      display: "flex",
      alignItems: "center",
      justifyContent: "center",
      fontFamily: "var(--f-body)",
      color: "var(--primary-text)",
    }}>
      {/* scrim grain */}
      <div style={{ position: "absolute", inset: 0,
        background: "repeating-linear-gradient(135deg, rgba(255,255,255,0.012) 0 2px, transparent 2px 8px)" }}/>

      {/* modal */}
      <div className="edge-iron-strong" style={{
        position: "relative",
        width: 1120, height: 700,
        background: "var(--charcoal-panel)",
        borderRadius: 3,
        display: "grid",
        gridTemplateRows: "48px 1fr 40px",
        boxShadow: "0 24px 60px rgba(0,0,0,0.7), 0 0 0 1px rgba(28,183,200,0.04)",
      }}>
        {/* header */}
        <div style={{
          display: "grid", gridTemplateColumns: "auto 1fr auto",
          alignItems: "center", padding: "0 16px",
          borderBottom: "1px solid var(--iron-edge)",
          gap: 16,
        }}>
          <div style={{ display: "flex", alignItems: "center", gap: 10 }}>
            <span style={{ fontFamily: "var(--f-title)", color: "var(--ember-gold)", fontSize: 18, letterSpacing: "0.10em" }}>背包</span>
            <span style={{ color: "var(--talent-locked)" }}>·</span>
            <span style={{ fontFamily: "var(--f-mono)", color: "var(--cold-cyan)", fontSize: 12 }}>INVENTORY</span>
            <span style={{ marginLeft: 12, fontFamily: "var(--f-mono)", color: "var(--muted-text)", fontSize: 12 }}>
              <span className="t-primary">20</span>
              <span style={{ opacity: 0.5 }}> / 40</span>
            </span>
            <span style={{ marginLeft: 4, fontFamily: "var(--f-mono)", color: "var(--muted-text)", fontSize: 11 }}>
              · 重量 <span className="t-primary">28.4</span><span style={{opacity:0.5}}>/60</span>kg
            </span>
          </div>

          <div style={{ display: "flex", gap: 4, justifyContent: "center" }}>
            {FILTERS.map(f => (
              <FilterTab key={f.k} f={f} active={f.k === activeFilter} />
            ))}
          </div>

          <div style={{ display: "flex", alignItems: "center", gap: 8 }}>
            <span style={{ fontFamily: "var(--f-mono)", color: "var(--muted-text)", fontSize: 11 }}>排序</span>
            <SortChip>类型</SortChip>
            <SortChip active>稀有</SortChip>
            <SortChip>新增</SortChip>
            <span style={{ width: 1, height: 16, background: "var(--iron-edge)", margin: "0 6px" }}/>
            <KeyCap>Esc</KeyCap>
          </div>
        </div>

        {/* body */}
        <div style={{ display: "grid", gridTemplateColumns: "1fr 360px", gap: 6, padding: 6, minHeight: 0 }}>
          {/* grid */}
          <Panel padding={12}>
            <div style={{
              display: "grid",
              gridTemplateColumns: "repeat(8, 72px)",
              gap: 8,
              justifyContent: "space-between",
            }}>
              {SAMPLE_ITEMS.slice(0, 32).map((item, idx) => (
                <InventoryCell key={idx} item={item} selected={item && item.id === selectedId} />
              ))}
            </div>

            {showOverlay && (
              <div style={{ marginTop: 12, display: "flex", gap: 6 }}>
                <SpecTag>slot=64 · cell=72</SpecTag>
                <SpecTag>gap=8 · 8 cols</SpecTag>
              </div>
            )}
          </Panel>

          {/* detail */}
          <Panel padding={0} edge="cyan-strip">
            {selected ? <ItemDetail item={selected} /> : <EmptyDetail/>}
          </Panel>
        </div>

        {/* footer */}
        <div style={{
          display: "flex", alignItems: "center", gap: 14,
          padding: "0 16px",
          borderTop: "1px solid var(--iron-edge)",
          fontSize: 11, color: "var(--muted-text)",
        }}>
          <LegendItem keys={["Enter"]} label="装备 / 使用" tone="gold" />
          <LegendItem keys={["D"]}     label="丢弃"        tone="gold" />
          <LegendItem keys={["S"]}     label="拆解"        tone="gold" />
          <LegendItem keys={["Tab"]}   label="切换筛选"    tone="gold" />
          <LegendItem keys={["←","→","↑","↓"]} label="选择" tone="cyan" />
          <LegendItem keys={["Esc"]}   label="关闭"        tone="iron" />
        </div>

        {/* tooltip popover anchored to selected cell */}
        {showTooltip && selected && (
          <div style={{
            position: "absolute",
            // anchor over the selected slot — for i08 (row 0 col 7) ≈ x:8col*80+72, y:50+12+0row*80
            top: 90, left: 720,
            zIndex: 10,
          }}>
            <ItemTooltip item={selected} />
          </div>
        )}

        {showOverlay && (
          <>
            <SpecTag style={{ position: "absolute", top: 56, left: 12 }}>header.h=48</SpecTag>
            <SpecTag style={{ position: "absolute", bottom: 50, left: 12 }}>footer.h=40</SpecTag>
            <SpecTag style={{ position: "absolute", top: 56, right: 380 }}>detail.col=360</SpecTag>
          </>
        )}
      </div>

      {showOverlay && (
        <SpecTag style={{ position: "absolute", top: 50, left: 90 }}>modal 1120×700 · scrim 0.6 · vignette</SpecTag>
      )}
    </div>
  );
}

function FilterTab({ f, active }) {
  return (
    <div style={{
      padding: "4px 10px",
      background: active ? "rgba(28,183,200,0.08)" : "transparent",
      border: `1px solid ${active ? "var(--cold-cyan)" : "var(--iron-edge-dim)"}`,
      borderRadius: 2,
      cursor: "default",
      display: "flex", alignItems: "center", gap: 6,
    }}>
      <span style={{ fontSize: 12, color: active ? "var(--cold-cyan)" : "var(--primary-text)" }}>{f.label}</span>
      <span style={{ fontFamily: "var(--f-mono)", fontSize: 10, color: "var(--muted-text)" }}>{f.count}</span>
    </div>
  );
}

function SortChip({ children, active = false }) {
  return (
    <span style={{
      padding: "2px 6px",
      background: active ? "rgba(217,154,43,0.08)" : "transparent",
      border: `1px solid ${active ? "var(--ember-gold)" : "var(--iron-edge)"}`,
      color: active ? "var(--ember-gold)" : "var(--muted-text)",
      fontSize: 11,
      borderRadius: 2,
    }}>{children}</span>
  );
}

function InventoryCell({ item, selected }) {
  if (!item) {
    return (
      <div style={{
        width: 72, height: 72,
        background: "rgba(5,7,10,0.4)",
        border: "1px solid var(--iron-edge-dim)",
        borderRadius: 2,
        position: "relative",
      }}>
        <CornerTicks size={72} color="var(--iron-edge-dim)" />
      </div>
    );
  }
  const r = RARITY[item.rarity];
  return (
    <div style={{
      position: "relative", width: 72, height: 72,
      background: "radial-gradient(60% 60% at 50% 40%, rgba(255,235,200,0.02), transparent 80%), #0A0E14",
      border: `1px solid ${selected ? "var(--cold-cyan)" : r.dim}`,
      boxShadow: selected
        ? "inset 0 0 0 1px rgba(28,183,200,0.25), 0 0 0 1px rgba(28,183,200,0.10)"
        : `inset 0 0 0 1px ${item.rarity === "common" ? "rgba(0,0,0,0.5)" : "rgba(0,0,0,0.6)"}`,
      borderRadius: 2,
    }}>
      <CornerTicks size={72} color={selected ? "var(--cold-cyan)" : r.dim} />
      <div style={{ position: "absolute", inset: 4 }}>
        <IconToken token={item.token} size={64} tone={
          item.rarity === "common" ? "iron" :
          item.rarity === "uncommon" ? "green" :
          item.rarity === "rare" ? "cyan" :
          item.rarity === "epic" ? "violet" :
          "gold"
        } fill="hollow" hint={item.slot} />
      </div>
      {/* rarity ribbon — top-left */}
      <div style={{
        position: "absolute", top: 0, left: 0, width: 6, height: 18,
        background: r.color,
        opacity: item.rarity === "common" ? 0.4 : 1,
      }}/>
      {/* count */}
      {item.count != null && (
        <div style={{
          position: "absolute", bottom: 2, right: 4,
          color: "var(--primary-text)",
          fontFamily: "var(--f-mono)", fontSize: 12, fontWeight: 600,
          textShadow: "0 1px 0 #000, 0 0 2px #000",
        }}>{item.count}</div>
      )}
      {/* equipped marker */}
      {item.equipped && (
        <div style={{
          position: "absolute", top: 2, right: 2,
          width: 14, height: 14,
          borderRadius: 1,
          background: "var(--void-black)",
          border: "1px solid var(--ember-gold)",
          color: "var(--ember-gold)",
          fontFamily: "var(--f-mono)", fontSize: 9, fontWeight: 700,
          display: "flex", alignItems: "center", justifyContent: "center",
          lineHeight: 1,
        }}>E</div>
      )}
      {/* broken marker */}
      {item.broken && (
        <div style={{
          position: "absolute", top: 2, right: 2,
          width: 14, height: 14,
          borderRadius: 1,
          background: "var(--void-black)",
          border: "1px solid var(--blood-red)",
          color: "var(--blood-red)",
          fontFamily: "var(--f-mono)", fontSize: 9, fontWeight: 700,
          display: "flex", alignItems: "center", justifyContent: "center",
          lineHeight: 1,
        }}>!</div>
      )}
    </div>
  );
}

function ItemDetail({ item }) {
  const r = RARITY[item.rarity];
  return (
    <div style={{ display: "flex", flexDirection: "column", height: "100%" }}>
      {/* identity bar */}
      <div style={{ padding: 14, borderBottom: "1px solid var(--iron-edge-dim)" }}>
        <div style={{ display: "flex", alignItems: "center", gap: 12 }}>
          <div style={{
            width: 64, height: 64,
            border: `1px solid ${r.dim}`,
            background: "#0A0E14",
            display: "flex", alignItems: "center", justifyContent: "center",
            borderRadius: 2,
          }}>
            <IconToken token={item.token} size={56} tone={item.rarity === "common" ? "iron" : item.rarity === "uncommon" ? "green" : item.rarity === "rare" ? "cyan" : item.rarity === "epic" ? "violet" : "gold"} fill="hollow" hint={item.slot}/>
          </div>
          <div style={{ flex: 1, minWidth: 0 }}>
            <div style={{ display: "flex", alignItems: "baseline", gap: 6 }}>
              <span style={{ fontFamily: "var(--f-title)", color: r.color, fontSize: 18, letterSpacing: "0.04em" }}>{item.name}</span>
              {item.equipped && (
                <span style={{ fontFamily: "var(--f-mono)", fontSize: 10, color: "var(--ember-gold)", border: "1px solid var(--ember-gold-dim)", padding: "0 4px", borderRadius: 1 }}>装备中</span>
              )}
            </div>
            <div style={{ color: "var(--muted-text)", fontSize: 11, marginTop: 2 }}>
              {item.slot} · Tier {item.tier} · <span style={{ color: r.color }}>{r.en}</span>
            </div>
            <div style={{ fontFamily: "var(--f-mono)", color: "var(--cold-cyan)", fontSize: 10, marginTop: 1 }}>{item.token}</div>
          </div>
        </div>
      </div>

      <div style={{ padding: 14, flex: 1, display: "flex", flexDirection: "column", gap: 12 }}>

        <DetailSection label="属性 · STATS">
          {item.stats ? (
            <div style={{ display: "grid", gridTemplateColumns: "auto 1fr", gap: "4px 14px", fontSize: 12 }}>
              {item.stats.map(s => (
                <React.Fragment key={s.k}>
                  <span style={{ color: "var(--muted-text)" }}>{s.k}</span>
                  <span style={{ color: "var(--primary-text)", fontFamily: "var(--f-mono)" }}>{s.v}</span>
                </React.Fragment>
              ))}
            </div>
          ) : (
            <div style={{ color: "var(--talent-locked)", fontSize: 11 }}>(无加成)</div>
          )}
        </DetailSection>

        <DetailSection label="对比 · COMPARE (vs 装备中)">
          <div style={{ display: "grid", gridTemplateColumns: "auto 60px 60px", gap: "4px 14px", fontSize: 12 }}>
            <span style={{ color: "var(--muted-text)" }}>攻击</span>
            <span className="t-mono t-primary" style={{textAlign:"right"}}>18</span>
            <span className="t-mono t-green"   style={{textAlign:"right"}}>+3</span>
            <span style={{ color: "var(--muted-text)" }}>暴击</span>
            <span className="t-mono t-primary" style={{textAlign:"right"}}>4%</span>
            <span className="t-mono t-red"     style={{textAlign:"right"}}>−1%</span>
          </div>
        </DetailSection>

        <DetailSection label="描述">
          <div style={{ fontSize: 11, lineHeight: 1.55, color: "var(--muted-text)", fontStyle: "italic" }}>
            刃身缠着深褐色的麻绳,被无数次握过。剑脊的小裂纹里还嵌着 哨所的红泥。
          </div>
        </DetailSection>

        <div style={{ marginTop: "auto", display: "flex", gap: 6, fontSize: 11, color: "var(--muted-text)" }}>
          <span>价值 <span className="t-gold t-mono">{item.tier * 24 + 18}</span></span>
          <span style={{opacity:0.4}}>·</span>
          <span>重量 <span className="t-primary t-mono">{(item.tier + 1) * 0.7}kg</span></span>
        </div>
      </div>

      <div style={{ padding: "10px 14px", borderTop: "1px solid var(--iron-edge-dim)", display: "flex", gap: 6 }}>
        <ActionButton tone="gold" primary>{item.equipped ? "Enter · 卸下" : "Enter · 装备"}</ActionButton>
        <ActionButton tone="iron">D · 丢弃</ActionButton>
      </div>
    </div>
  );
}

function EmptyDetail() {
  return (
    <div style={{ padding: 24, color: "var(--talent-locked)", fontSize: 12, textAlign: "center" }}>
      <div style={{ fontFamily: "var(--f-mono)", marginBottom: 6 }}>[detail.empty]</div>
      <div>选择一件物品以查看详情</div>
    </div>
  );
}

// ====== Item Tooltip — hover popover ======
function ItemTooltip({ item, position = "right" }) {
  const r = RARITY[item.rarity];
  return (
    <div style={{
      width: 280,
      background: "var(--charcoal-panel)",
      border: `1px solid ${r.dim}`,
      boxShadow: `0 12px 30px rgba(0,0,0,0.6), 0 0 0 1px ${r.color}22, inset 0 0 0 1px rgba(0,0,0,0.6)`,
      borderRadius: 3,
      overflow: "hidden",
      fontFamily: "var(--f-body)",
      color: "var(--primary-text)",
    }}>
      {/* rarity bar */}
      <div style={{ height: 3, background: r.color, opacity: item.rarity === "common" ? 0.4 : 1 }}/>
      {/* header */}
      <div style={{ padding: "8px 12px", borderBottom: "1px solid var(--iron-edge-dim)", background: `linear-gradient(180deg, ${r.dim}22, transparent)` }}>
        <div style={{ fontFamily: "var(--f-title)", color: r.color, fontSize: 15, letterSpacing: "0.04em", textWrap: "balance" }}>{item.name}</div>
        <div style={{ color: "var(--muted-text)", fontFamily: "var(--f-mono)", fontSize: 10, marginTop: 1 }}>
          {item.slot} · T{item.tier} · {r.en}
        </div>
      </div>
      {/* stats */}
      {item.stats && (
        <div style={{ padding: "8px 12px", borderBottom: "1px dashed var(--iron-edge-dim)" }}>
          {item.stats.map(s => (
            <div key={s.k} style={{ display: "flex", justifyContent: "space-between", fontSize: 12, lineHeight: 1.6 }}>
              <span style={{ color: "var(--muted-text)" }}>{s.k}</span>
              <span style={{ color: "var(--stamina-green)", fontFamily: "var(--f-mono)" }}>{s.v}</span>
            </div>
          ))}
        </div>
      )}
      {/* affix */}
      {item.rarity !== "common" && (
        <div style={{ padding: "8px 12px", borderBottom: "1px dashed var(--iron-edge-dim)" }}>
          <div style={{ fontSize: 11, color: "var(--cold-cyan)", marginBottom: 2 }}>◆ 流光铭刻</div>
          <div style={{ fontSize: 11, color: "var(--muted-text)" }}>每次暴击恢复 <span className="t-cyan">2 SP</span>。</div>
        </div>
      )}
      {/* set bonus */}
      {item.rarity === "epic" && (
        <div style={{ padding: "8px 12px", borderBottom: "1px dashed var(--iron-edge-dim)" }}>
          <div style={{ fontSize: 11, color: "var(--arcane-violet)", marginBottom: 2 }}>套装 · 虚空使徒 (2/4)</div>
          <div style={{ fontSize: 11, color: "var(--muted-text)" }}>
            <span className="t-violet">2 件</span>:奥术抗性 +12<br/>
            <span style={{color:"var(--talent-locked)"}}>4 件:每秒回复 1 SP</span>
          </div>
        </div>
      )}
      {/* flavor */}
      <div style={{ padding: "8px 12px", fontSize: 11, fontStyle: "italic", color: "var(--talent-locked)", lineHeight: 1.55 }}>
        "{item.rarity === "epic" ? "在虚空的呼吸里听清自己的名字。" : item.rarity === "rare" ? "暮光不是黑暗,是被夺走的光。" : "刃身缠着深褐色的麻绳。"}"
      </div>
      {/* footer */}
      <div style={{
        display: "flex", justifyContent: "space-between",
        padding: "6px 12px",
        background: "rgba(5,7,10,0.5)",
        borderTop: "1px solid var(--iron-edge-dim)",
        fontSize: 10, color: "var(--muted-text)", fontFamily: "var(--f-mono)",
      }}>
        <span>价值 <span className="t-gold">{item.tier * 24 + 18}</span></span>
        <span>重 <span className="t-primary">{(item.tier + 1) * 0.7}</span>kg</span>
      </div>
    </div>
  );
}

// ====== Tooltip showcase — 4 rarity variants ======
function TooltipShowcase() {
  const items = [
    { id: "t1", name: "古旧短剑",     slot: "weapon", tier: 1, rarity: "common",
      token: "eq.sword_t1", stats: [{k:"攻击", v:"+12 物理"}] },
    { id: "t2", name: "矿工战镐 +1",  slot: "weapon", tier: 1, rarity: "uncommon",
      token: "eq.pick_t1",  stats: [{k:"攻击", v:"+15 物理"},{k:"破甲", v:"+1"}] },
    { id: "t3", name: "暮光斗篷",     slot: "cloak",  tier: 2, rarity: "rare",
      token: "eq.cloak_t2", stats: [{k:"闪避", v:"+8%"},{k:"暗影抗性", v:"+12"}] },
    { id: "t4", name: "护符·虚空",    slot: "amulet", tier: 3, rarity: "epic",
      token: "eq.amulet_t3",stats: [{k:"奥术", v:"+22"},{k:"耐力上限", v:"+10"}] },
    { id: "t5", name: "Vyon 的誓刃",  slot: "weapon", tier: 4, rarity: "artifact",
      token: "eq.sword_art",stats: [{k:"攻击", v:"+34 物理"},{k:"暴击", v:"+10%"},{k:"破甲", v:"+6"}] },
  ];
  return (
    <div style={{ display: "flex", flexWrap: "wrap", gap: 16, padding: 16 }}>
      {items.map(it => (
        <div key={it.id} style={{ display: "flex", flexDirection: "column", alignItems: "center", gap: 8 }}>
          <ItemTooltip item={it} />
          <div style={{ fontFamily: "var(--f-mono)", fontSize: 10, color: "var(--cold-cyan)" }}>rarity.{it.rarity}</div>
        </div>
      ))}
    </div>
  );
}

window.InventoryModal = InventoryModal;
window.ItemTooltip = ItemTooltip;
window.TooltipShowcase = TooltipShowcase;
window.RARITY = RARITY;
