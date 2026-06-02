// components/main-menu.jsx
// K-ToME Main Menu — redesign of the start screen.
// Style: ktome-dark-fantasy-sprite-ui-v1 (ART_STYLE_BIBLE §2-4).
// Forged-iron chrome, ember-gold masthead, cyan focus edges, worn-stone foundry.
// Presentational; all selection state owned by host App (keyboard + Tweaks sync).

// ===== Data =====
const MM_CLASSES = [
  {
    key: "vanguard", zh: "战卫", en: "Vanguard", token: "portrait.vanguard", tone: "gold",
    role: "持盾近战 · 指挥型", resource: "耐力",
    resourceNote: "稳定的近战资源,没有额外循环。",
    blurb: "纪律严明的前线战士,会把小优势稳定地滚成胜势。",
    sub: [{ zh: "狂战士", en: "Berserker" }, { zh: "咒剑士", en: "Spellblade" }],
    stats: { atk: 0.74, def: 0.86, res: 0.62 }, ready: true,
  },
  {
    key: "rogue", zh: "游荡者", en: "Rogue", token: "portrait.rogue", tone: "cyan",
    role: "高机动近战 · 爆发型", resource: "集中",
    resourceNote: "靠连击积攒,空窗会清零。",
    blurb: "影里出手的猎手,一次失误也可能是一次处决。",
    sub: [{ zh: "刺客", en: "Assassin" }, { zh: "游侠", en: "Ranger" }],
    stats: { atk: 0.9, def: 0.5, res: 0.7 }, ready: true,
  },
  {
    key: "templar", zh: "圣堂", en: "Templar", token: "portrait.templar", tone: "gold",
    role: "神圣坦克 · 守护型", resource: "信仰",
    resourceNote: "受击与守护积累,惩戒时倾泻。",
    blurb: "以誓言铸盾的守誓者,越是绝境越是坚硬。",
    sub: [{ zh: "审判官", en: "Inquisitor" }, { zh: "守誓者", en: "Warden" }],
    stats: { atk: 0.6, def: 0.92, res: 0.74 }, ready: true,
  },
  {
    key: "arcanist", zh: "秘术师", en: "Arcanist", token: "portrait.arcanist", tone: "violet",
    role: "远程法术 · 控制型", resource: "法力",
    resourceNote: "缓慢回复,需要节奏管理。",
    blurb: "在虚空边缘借力的咒者,代价从不延迟兑现。",
    sub: [{ zh: "元素师", en: "Elementalist" }, { zh: "咒术师", en: "Hexer" }],
    stats: { atk: 0.82, def: 0.44, res: 0.95 }, ready: true,
  },
];

const MM_RACES = [
  { key: "human", zh: "人类", en: "Human", note: "通才 · 无短板" },
  { key: "dwarf", zh: "矮人", en: "Dwarf", note: "坚韧 · 抗性强" },
  { key: "elf",   zh: "精灵", en: "Elf",   note: "敏捷 · 资源快" },
  { key: "orc",   zh: "兽人", en: "Orc",   note: "蛮力 · 高伤害" },
];

const MM_MENU = [
  { key: "quickstart", zh: "快速开始", en: "QUICK START", sub: "以当前职业 / 种族立即进入", tone: "gold", primary: true },
  { key: "continue",   zh: "继续游戏", en: "CONTINUE",    sub: "读取最近的局间存档", tone: "cyan" },
  { key: "validation", zh: "验证模式", en: "VALIDATION",  sub: "次级路径 · 调试与回归", tone: "iron" },
  { key: "settings",   zh: "设置",     en: "SETTINGS",    sub: "画面 · 音频 · 按键", tone: "iron" },
  { key: "quit",       zh: "退出",     en: "QUIT",        sub: "结束本次运行", tone: "iron" },
];

function MainMenu({
  selectedMenu = "quickstart",
  classIndex = 0,
  raceIndex = 0,
  showError = true,
  showOverlay = false,
  lang = "简体中文",
  onSelectMenu = () => {},
  onSelectClass = () => {},
  onSelectRace = () => {},
}) {
  const cls = MM_CLASSES[classIndex];
  const race = MM_RACES[raceIndex];

  return (
    <div data-screen-label="main_menu" style={{
      position: "relative",
      width: 1280, height: 800,
      background:
        "radial-gradient(120% 90% at 50% -10%, rgba(217,154,43,0.06), transparent 55%)," +
        "radial-gradient(100% 80% at 50% 120%, rgba(28,183,200,0.04), transparent 60%)," +
        "var(--void-black)",
      fontFamily: "var(--f-body)",
      color: "var(--primary-text)",
      display: "grid",
      gridTemplateRows: "150px 1fr auto",
      gap: 14,
      padding: 28,
      boxSizing: "border-box",
      overflow: "hidden",
    }}>
      {/* atmospheric grit overlay */}
      <div style={{ position: "absolute", inset: 0, pointerEvents: "none",
        background: "repeating-linear-gradient(115deg, rgba(255,250,235,0.010) 0 2px, transparent 2px 10px)" }}/>
      {/* deep vignette */}
      <div style={{ position: "absolute", inset: 0, pointerEvents: "none",
        background: "radial-gradient(130% 100% at 50% 45%, transparent 50%, rgba(0,0,0,0.65) 100%)" }}/>

      {/* ===== Masthead ===== */}
      <MMMasthead showOverlay={showOverlay} />

      {/* ===== Body ===== */}
      <div style={{ display: "grid", gridTemplateColumns: "380px 1fr", gap: 14, minHeight: 0, position: "relative" }}>
        {/* --- Menu rail --- */}
        <MMFrame surface="charcoal">
          <div style={{ padding: 18, height: "100%", display: "flex", flexDirection: "column" }}>
            <MMSectionLabel zh="主菜单" en="NAVIGATION" />
            <div style={{ display: "flex", flexDirection: "column", gap: 8, marginTop: 12 }}>
              {MM_MENU.map(m => (
                <MMMenuItem key={m.key} item={m}
                  selected={m.key === selectedMenu}
                  disabled={m.key === "continue" && showError}
                  onClick={() => onSelectMenu(m.key)} />
              ))}
            </div>
            <div style={{ marginTop: "auto", paddingTop: 14, borderTop: "1px solid var(--iron-edge-dim)",
              display: "flex", justifyContent: "space-between", alignItems: "center",
              fontFamily: "var(--f-mono)", fontSize: 10, color: "var(--talent-locked)" }}>
              <span>build · phase4</span>
              <span>v1.0.0-rc · ktome</span>
            </div>
            {showOverlay && <SpecTag style={{ position: "absolute", top: 4, left: 4 }}>rail.w=380</SpecTag>}
          </div>
        </MMFrame>

        {/* --- Character foundry --- */}
        <MMFrame surface="stone">
          <div style={{ padding: 18, height: "100%", display: "flex", flexDirection: "column", minHeight: 0 }}>
            <div style={{ display: "flex", justifyContent: "space-between", alignItems: "baseline" }}>
              <MMSectionLabel zh="角色" en="CHARACTER FOUNDRY" />
              <div style={{ display: "flex", gap: 12, alignItems: "center", fontSize: 11, color: "var(--muted-text)" }}>
                <span><KeyCap tone="cyan">←</KeyCap><KeyCap tone="cyan" style={{ marginLeft: 2 }}>→</KeyCap> 切换职业</span>
                <span><KeyCap tone="gold">Q</KeyCap><KeyCap tone="gold" style={{ marginLeft: 2 }}>E</KeyCap> 切换种族</span>
              </div>
            </div>

            {/* identity row */}
            <div style={{ display: "grid", gridTemplateColumns: "220px 1fr", gap: 18, marginTop: 14, minHeight: 0, flex: 1 }}>
              {/* portrait */}
              <MMPortrait cls={cls} showOverlay={showOverlay} />

              {/* identity copy */}
              <div style={{ display: "flex", flexDirection: "column", minWidth: 0 }}>
                <div style={{ display: "flex", alignItems: "baseline", gap: 10, flexWrap: "wrap" }}>
                  <span style={{ fontFamily: "var(--f-title)", color: "var(--ember-gold)", fontSize: 32, letterSpacing: "0.04em", lineHeight: 1 }}>{cls.zh}</span>
                  <span style={{ fontFamily: "var(--f-title)", color: "var(--muted-text)", fontSize: 16, letterSpacing: "0.12em" }}>{cls.en}</span>
                  <MMReadyChip ready={cls.ready} />
                </div>
                <div style={{ color: "var(--muted-text)", fontSize: 12, marginTop: 4 }}>{cls.role} · 种族 <span className="t-primary">{race.zh}</span></div>

                <div style={{ display: "grid", gridTemplateColumns: "auto 1fr", gap: "6px 12px", marginTop: 14, fontSize: 13, alignItems: "baseline" }}>
                  <span style={{ color: "var(--ember-gold)", fontFamily: "var(--f-title)", fontSize: 11, letterSpacing: "0.1em" }}>资源</span>
                  <span className="t-primary">{cls.resource} — <span className="t-muted">{cls.resourceNote}</span></span>
                  <span style={{ color: "var(--ember-gold)", fontFamily: "var(--f-title)", fontSize: 11, letterSpacing: "0.1em" }}>定位</span>
                  <span className="t-muted" style={{ lineHeight: 1.5 }}>{cls.blurb}</span>
                </div>

                {/* stat preview */}
                <div style={{ display: "flex", gap: 18, marginTop: 14 }}>
                  <MMStatMeter label="攻击" value={cls.stats.atk} tone="red" />
                  <MMStatMeter label="防御" value={cls.stats.def} tone="iron" />
                  <MMStatMeter label="资源" value={cls.stats.res} tone="green" />
                </div>

                {/* discovered subclasses */}
                <div style={{ marginTop: "auto", paddingTop: 14 }}>
                  <div style={{ fontFamily: "var(--f-title)", fontSize: 10, letterSpacing: "0.14em", color: "var(--ember-gold)", marginBottom: 6 }}>已发现 · DISCOVERED</div>
                  <div style={{ display: "flex", gap: 8 }}>
                    {cls.sub.map(s => (
                      <div key={s.en} style={{ display: "flex", alignItems: "center", gap: 6, padding: "4px 10px",
                        border: "1px dashed var(--talent-locked)", borderRadius: 2, background: "rgba(5,7,10,0.5)" }}>
                        <IconToken token={`talent.${s.en.toLowerCase()}`} size={18} tone="locked" fill="hollow" showLabel={false} />
                        <span style={{ fontSize: 12, color: "var(--muted-text)" }}>{s.zh}</span>
                        <span style={{ fontFamily: "var(--f-mono)", fontSize: 9, color: "var(--talent-locked)" }}>locked</span>
                      </div>
                    ))}
                    <span style={{ alignSelf: "center", fontSize: 11, color: "var(--talent-locked)" }}>暂不放入正式开局主路径</span>
                  </div>
                </div>
              </div>
            </div>

            {/* roster selectors */}
            <div style={{ display: "grid", gridTemplateColumns: "1fr 320px", gap: 14, marginTop: 14, paddingTop: 14, borderTop: "1px solid var(--iron-edge-dim)" }}>
              {/* class roster */}
              <div>
                <div style={{ fontFamily: "var(--f-title)", fontSize: 10, letterSpacing: "0.14em", color: "var(--muted-text)", marginBottom: 8 }}>基础职业 · 4 PLAYABLE</div>
                <div style={{ display: "flex", gap: 10 }}>
                  {MM_CLASSES.map((c, i) => (
                    <MMRosterSlot key={c.key} cls={c} selected={i === classIndex} onClick={() => onSelectClass(i)} />
                  ))}
                </div>
              </div>
              {/* race roster */}
              <div>
                <div style={{ fontFamily: "var(--f-title)", fontSize: 10, letterSpacing: "0.14em", color: "var(--muted-text)", marginBottom: 8 }}>基础种族 · ENABLED</div>
                <div style={{ display: "flex", gap: 6 }}>
                  {MM_RACES.map((r, i) => (
                    <MMRaceChip key={r.key} race={r} selected={i === raceIndex} onClick={() => onSelectRace(i)} />
                  ))}
                </div>
              </div>
            </div>
            {showOverlay && <SpecTag style={{ position: "absolute", top: 4, right: 4 }}>foundry · stone 9p</SpecTag>}
          </div>
        </MMFrame>
      </div>

      {/* ===== Bottom: warning + command bar ===== */}
      <div style={{ display: "flex", flexDirection: "column", gap: 10 }}>
        {showError && <MMWarningBanner />}
        <MMCommandBar lang={lang} />
      </div>
    </div>
  );
}

// ===== Masthead =====
function MMMasthead({ showOverlay }) {
  return (
    <MMFrame surface="charcoal">
      <div style={{ position: "relative", height: "100%", display: "flex", alignItems: "center", padding: "0 26px", overfl: "hidden", overflow: "hidden" }}>
        {/* ghosted hero silhouette on the right */}
        <div style={{ position: "absolute", right: 30, top: 0, bottom: 0, width: 360, pointerEvents: "none",
          background: "radial-gradient(70% 90% at 70% 50%, rgba(217,154,43,0.10), transparent 70%)" }}/>
        <div style={{ position: "absolute", right: 60, top: "50%", transform: "translateY(-50%)", pointerEvents: "none", opacity: 0.5 }}>
          <IconToken token="brand.hero_silhouette" size={104} tone="iron" fill="hollow" hint="masthead art" />
        </div>

        {/* wordmark */}
        <div style={{ position: "relative" }}>
          <div style={{ display: "flex", alignItems: "baseline", gap: 14 }}>
            <span style={{
              fontFamily: "var(--f-title)",
              fontSize: 64, fontWeight: 700,
              color: "var(--ember-gold)",
              letterSpacing: "0.06em",
              lineHeight: 1,
              textShadow: "0 0 30px rgba(217,154,43,0.35), 0 2px 0 rgba(0,0,0,0.8)",
            }}>K-ToME</span>
            <span style={{ fontFamily: "var(--f-mono)", fontSize: 12, color: "var(--cold-cyan)", letterSpacing: "0.15em", border: "1px solid var(--cold-cyan-dim)", padding: "2px 6px", borderRadius: 2 }}>ROGUELIKE</span>
          </div>
          <div style={{ display: "flex", alignItems: "center", gap: 12, marginTop: 12 }}>
            <span style={{ width: 40, height: 1, background: "linear-gradient(to right, var(--ember-gold), transparent)" }}/>
            <span style={{ fontFamily: "var(--f-title)", fontSize: 18, color: "var(--primary-text)", letterSpacing: "0.34em" }}>主 菜 单</span>
            <span style={{ fontFamily: "var(--f-mono)", fontSize: 11, color: "var(--muted-text)", letterSpacing: "0.18em" }}>MAIN MENU</span>
          </div>
        </div>
        {showOverlay && <SpecTag style={{ position: "absolute", bottom: 4, left: 4 }}>masthead.h=150</SpecTag>}
      </div>
    </MMFrame>
  );
}

// ===== Forged iron frame with corner brackets + rivets =====
function MMFrame({ children, surface = "charcoal", style = {} }) {
  const bg =
    surface === "stone"
      ? "repeating-linear-gradient(115deg, rgba(255,250,235,0.012) 0 2px, transparent 2px 9px)," +
        "repeating-linear-gradient(25deg, rgba(0,0,0,0.16) 0 1px, transparent 1px 11px)," +
        "radial-gradient(140% 100% at 50% 0%, rgba(217,154,43,0.035), transparent 60%)," +
        "var(--charcoal-panel)"
      : "radial-gradient(120% 80% at 30% 0%, rgba(255,235,200,0.020), transparent 60%)," +
        "radial-gradient(80% 100% at 80% 100%, rgba(0,0,0,0.45), transparent 60%)," +
        "var(--charcoal-panel)";
  return (
    <div style={{
      position: "relative",
      background: bg,
      border: "1px solid var(--iron-edge)",
      borderRadius: 3,
      boxShadow: "inset 0 0 0 1px rgba(0,0,0,0.55), inset 0 0 40px rgba(0,0,0,0.45), 0 2px 0 rgba(0,0,0,0.6)",
      ...style,
    }}>
      {/* inner hairline */}
      <div style={{ position: "absolute", inset: 4, border: "1px solid rgba(217,154,43,0.10)", borderRadius: 2, pointerEvents: "none" }}/>
      {/* corner brackets */}
      {[
        { top: 2, left: 2, bt: 1, bl: 1 },
        { top: 2, right: 2, bt: 1, br: 1 },
        { bottom: 2, left: 2, bb: 1, bl: 1 },
        { bottom: 2, right: 2, bb: 1, br: 1 },
      ].map((c, i) => (
        <div key={i} style={{
          position: "absolute", width: 16, height: 16,
          top: c.top, left: c.left, right: c.right, bottom: c.bottom,
          borderTop: c.bt ? "2px solid var(--ember-gold-dim)" : "none",
          borderBottom: c.bb ? "2px solid var(--ember-gold-dim)" : "none",
          borderLeft: c.bl ? "2px solid var(--ember-gold-dim)" : "none",
          borderRight: c.br ? "2px solid var(--ember-gold-dim)" : "none",
          pointerEvents: "none",
        }}/>
      ))}
      {/* rivets */}
      {[
        { top: 8, left: 8 }, { top: 8, right: 8 }, { bottom: 8, left: 8 }, { bottom: 8, right: 8 },
      ].map((p, i) => (
        <div key={i} style={{ position: "absolute", width: 4, height: 4, borderRadius: "50%",
          background: "radial-gradient(circle at 30% 30%, #8a7a4b, #2a210e 70%)",
          boxShadow: "0 0 0 1px rgba(0,0,0,0.7)", ...p, pointerEvents: "none" }}/>
      ))}
      {children}
    </div>
  );
}

function MMSectionLabel({ zh, en }) {
  return (
    <div style={{ display: "flex", alignItems: "center", gap: 10 }}>
      <span style={{ fontFamily: "var(--f-title)", color: "var(--ember-gold)", fontSize: 14, letterSpacing: "0.14em" }}>{zh}</span>
      <span style={{ fontFamily: "var(--f-mono)", color: "var(--muted-text)", fontSize: 10, letterSpacing: "0.1em" }}>{en}</span>
      <span style={{ flex: 1, height: 1, background: "linear-gradient(to right, var(--ember-gold-dim), transparent 75%)" }}/>
    </div>
  );
}

// ===== Menu item =====
function MMMenuItem({ item, selected, disabled, onClick }) {
  const accent =
    item.tone === "gold" ? "var(--ember-gold)" :
    item.tone === "cyan" ? "var(--cold-cyan)" : "var(--iron-edge)";
  return (
    <button onClick={disabled ? undefined : onClick} disabled={disabled} style={{
      textAlign: "left",
      position: "relative",
      display: "flex", alignItems: "center", gap: 12,
      padding: "12px 14px",
      background: selected
        ? "linear-gradient(90deg, rgba(28,183,200,0.10), rgba(28,183,200,0.02) 60%, transparent)"
        : "linear-gradient(180deg, rgba(16,21,29,0.6), rgba(8,11,17,0.6))",
      border: `1px solid ${selected ? "var(--cold-cyan)" : "var(--iron-edge)"}`,
      borderLeft: selected ? "3px solid var(--cold-cyan)" : "3px solid var(--iron-edge-dim)",
      borderRadius: 2,
      cursor: disabled ? "not-allowed" : "pointer",
      opacity: disabled ? 0.4 : 1,
      boxShadow: selected ? "inset 0 0 0 1px rgba(28,183,200,0.18), 0 0 18px rgba(28,183,200,0.08)" : "inset 0 1px 0 rgba(255,255,255,0.02)",
      transition: "all 0.12s ease",
    }}>
      {/* selection rune */}
      <span style={{ width: 16, fontFamily: "var(--f-mono)", fontSize: 14, fontWeight: 700,
        color: selected ? "var(--cold-cyan)" : "var(--talent-locked)" }}>{selected ? "▶" : "◆"}</span>
      <div style={{ flex: 1, minWidth: 0 }}>
        <div style={{ display: "flex", alignItems: "baseline", gap: 8 }}>
          <span style={{ fontFamily: "var(--f-title)", fontSize: 17, letterSpacing: "0.06em",
            color: selected ? "var(--primary-text)" : (item.primary ? "var(--ember-gold)" : "var(--primary-text)") }}>{item.zh}</span>
          <span style={{ fontFamily: "var(--f-mono)", fontSize: 10, letterSpacing: "0.12em",
            color: selected ? "var(--cold-cyan)" : "var(--muted-text)" }}>{item.en}</span>
        </div>
        <div style={{ fontSize: 11, color: "var(--muted-text)", marginTop: 1 }}>
          {disabled ? "无可用存档" : item.sub}
        </div>
      </div>
      {item.primary && !disabled && (
        <span style={{ fontFamily: "var(--f-mono)", fontSize: 10, color: "var(--ember-gold)",
          border: "1px solid var(--ember-gold-dim)", padding: "2px 5px", borderRadius: 2 }}>↵</span>
      )}
    </button>
  );
}

// ===== Portrait =====
function MMPortrait({ cls, showOverlay }) {
  const toneColor =
    cls.tone === "gold" ? "var(--ember-gold)" :
    cls.tone === "cyan" ? "var(--cold-cyan)" : "var(--arcane-violet)";
  return (
    <div style={{ position: "relative", display: "flex", flexDirection: "column" }}>
      <div style={{
        position: "relative", flex: 1, minHeight: 200,
        border: `1px solid ${toneColor}`,
        background: "radial-gradient(70% 55% at 50% 30%, rgba(217,154,43,0.10), transparent 65%), linear-gradient(180deg, #11151c 0%, #0a0d13 60%, #07090d 100%)",
        borderRadius: 2,
        boxShadow: `inset 0 0 0 1px rgba(0,0,0,0.6), inset 0 0 28px rgba(0,0,0,0.7), 0 0 0 1px ${toneColor}22`,
        overflow: "hidden",
        display: "flex", alignItems: "center", justifyContent: "center",
      }}>
        {/* tile grid floor hint */}
        <div style={{ position: "absolute", left: 0, right: 0, bottom: 0, height: "30%",
          background: "repeating-linear-gradient(90deg, rgba(255,255,255,0.03) 0 1px, transparent 1px 24px), linear-gradient(to top, rgba(0,0,0,0.5), transparent)" }}/>
        <IconToken token={cls.token} size={120} tone={cls.tone} fill="hollow" hint="portrait · 256²" />
        {/* corner brackets */}
        {[{top:6,left:6,bt:1,bl:1},{top:6,right:6,bt:1,br:1},{bottom:6,left:6,bb:1,bl:1},{bottom:6,right:6,bb:1,br:1}].map((c,i)=>(
          <div key={i} style={{ position:"absolute", width:12, height:12, top:c.top,left:c.left,right:c.right,bottom:c.bottom,
            borderTop:c.bt?`1px solid ${toneColor}`:"none", borderBottom:c.bb?`1px solid ${toneColor}`:"none",
            borderLeft:c.bl?`1px solid ${toneColor}`:"none", borderRight:c.br?`1px solid ${toneColor}`:"none" }}/>
        ))}
      </div>
      {/* nameplate */}
      <div style={{ marginTop: 8, display: "flex", justifyContent: "space-between", alignItems: "center",
        padding: "6px 10px", background: "rgba(5,7,10,0.6)", border: "1px solid var(--iron-edge)", borderRadius: 2 }}>
        <span style={{ fontFamily: "var(--f-mono)", fontSize: 10, color: toneColor }}>{cls.token}</span>
        <span style={{ fontFamily: "var(--f-mono)", fontSize: 10, color: "var(--muted-text)" }}>{`${MM_CLASSES.indexOf(cls)+1}/4`}</span>
      </div>
      {showOverlay && <SpecTag style={{ position: "absolute", top: 4, left: 4 }}>portrait 220w</SpecTag>}
    </div>
  );
}

function MMReadyChip({ ready }) {
  return (
    <span style={{ display: "inline-flex", alignItems: "center", gap: 5, padding: "3px 9px",
      border: `1px solid ${ready ? "var(--stamina-green)" : "var(--blood-red)"}`,
      background: ready ? "rgba(82,201,137,0.08)" : "rgba(182,66,66,0.08)",
      borderRadius: 2 }}>
      <span style={{ width: 6, height: 6, borderRadius: "50%", background: ready ? "var(--stamina-green)" : "var(--blood-red)",
        boxShadow: ready ? "0 0 6px var(--stamina-green)" : "0 0 6px var(--blood-red)" }}/>
      <span style={{ fontSize: 11, color: ready ? "var(--stamina-green)" : "var(--blood-red)" }}>{ready ? "可立即开局" : "未就绪"}</span>
    </span>
  );
}

function MMStatMeter({ label, value, tone }) {
  const color = tone === "red" ? "var(--blood-red)" : tone === "green" ? "var(--stamina-green)" : "var(--cold-cyan)";
  const pips = 10, on = Math.round(value * pips);
  return (
    <div>
      <div style={{ display: "flex", justifyContent: "space-between", marginBottom: 4 }}>
        <span style={{ fontSize: 11, color: "var(--muted-text)" }}>{label}</span>
        <span style={{ fontFamily: "var(--f-mono)", fontSize: 11, color }}>{Math.round(value * 100)}</span>
      </div>
      <div style={{ display: "flex", gap: 2 }}>
        {Array.from({ length: pips }).map((_, i) => (
          <div key={i} style={{ width: 7, height: 10, borderRadius: 1,
            background: i < on ? color : "transparent",
            border: `1px solid ${i < on ? color : "var(--iron-edge)"}`,
            opacity: i < on ? 1 : 0.5 }}/>
        ))}
      </div>
    </div>
  );
}

// ===== Roster slots =====
function MMRosterSlot({ cls, selected, onClick }) {
  const toneColor = cls.tone === "gold" ? "var(--ember-gold)" : cls.tone === "cyan" ? "var(--cold-cyan)" : "var(--arcane-violet)";
  return (
    <button onClick={onClick} style={{
      flex: 1, display: "flex", flexDirection: "column", alignItems: "center", gap: 6,
      padding: "10px 6px 8px",
      background: selected ? "rgba(28,183,200,0.06)" : "rgba(5,7,10,0.45)",
      border: `1px solid ${selected ? "var(--cold-cyan)" : "var(--iron-edge)"}`,
      borderRadius: 2, cursor: "pointer",
      boxShadow: selected ? "inset 0 0 0 1px rgba(28,183,200,0.2), 0 0 14px rgba(28,183,200,0.08)" : "none",
      transition: "all 0.12s ease",
    }}>
      <IconToken token={cls.token} size={44} tone={selected ? cls.tone : "iron"} fill="hollow" showLabel={false} />
      <span style={{ fontFamily: "var(--f-title)", fontSize: 13, color: selected ? "var(--primary-text)" : "var(--muted-text)", letterSpacing: "0.04em" }}>{cls.zh}</span>
      <span style={{ fontFamily: "var(--f-mono)", fontSize: 8, color: selected ? "var(--cold-cyan)" : "var(--talent-locked)", letterSpacing: "0.08em" }}>{cls.en}</span>
    </button>
  );
}

function MMRaceChip({ race, selected, onClick }) {
  return (
    <button onClick={onClick} style={{
      flex: 1, display: "flex", flexDirection: "column", alignItems: "center", gap: 2,
      padding: "8px 4px",
      background: selected ? "rgba(217,154,43,0.08)" : "rgba(5,7,10,0.45)",
      border: `1px solid ${selected ? "var(--ember-gold)" : "var(--iron-edge)"}`,
      borderRadius: 2, cursor: "pointer",
      transition: "all 0.12s ease",
    }}>
      <span style={{ fontFamily: "var(--f-title)", fontSize: 13, color: selected ? "var(--ember-gold)" : "var(--muted-text)" }}>{race.zh}</span>
      <span style={{ fontFamily: "var(--f-mono)", fontSize: 8, color: selected ? "var(--primary-text)" : "var(--talent-locked)" }}>{race.note}</span>
    </button>
  );
}

// ===== Warning banner =====
function MMWarningBanner() {
  return (
    <div style={{ position: "relative", display: "flex", alignItems: "center", gap: 12,
      padding: "12px 18px",
      background: "linear-gradient(90deg, rgba(182,66,66,0.10), rgba(77,28,28,0.04))",
      border: "1px solid var(--blood-red)",
      borderRadius: 3,
      boxShadow: "inset 0 0 0 1px rgba(182,66,66,0.12), 0 0 16px rgba(182,66,66,0.06)" }}>
      <span style={{ width: 22, height: 22, flex: "0 0 22px", borderRadius: 2,
        border: "1px solid var(--blood-red)", color: "var(--blood-red)",
        display: "flex", alignItems: "center", justifyContent: "center",
        fontFamily: "var(--f-mono)", fontWeight: 700, fontSize: 14 }}>!</span>
      <div style={{ flex: 1 }}>
        <span style={{ color: "var(--blood-red)", fontSize: 13, fontWeight: 500 }}>局间档案加载失败。</span>
        <span style={{ color: "var(--muted-text)", fontSize: 13, marginLeft: 6 }}>现有进度文件未被覆盖,本次运行也不会写回新的局间进度。</span>
      </div>
      <span style={{ fontFamily: "var(--f-mono)", fontSize: 10, color: "var(--blood-red)",
        border: "1px solid var(--blood-red-dim)", padding: "2px 6px", borderRadius: 2 }}>SAVE · READ-ONLY</span>
    </div>
  );
}

// ===== Command bar =====
function MMCommandBar({ lang }) {
  const hints = [
    { keys: ["↑","↓"], label: "选择", tone: "cyan" },
    { keys: ["←","→"], label: "切换职业", tone: "cyan" },
    { keys: ["Q","E"], label: "切换种族", tone: "gold" },
    { keys: ["↵"],     label: "确认", tone: "gold" },
    { keys: ["L"],     label: "切换语言", tone: "iron" },
  ];
  const icons = [
    { token: "ui.back",    title: "返回" },
    { token: "ui.confirm", title: "确认", tone: "gold" },
    { token: "ui.journal", title: "图鉴" },
  ];
  return (
    <MMFrame surface="charcoal" style={{ borderRadius: 3 }}>
      <div style={{ display: "flex", alignItems: "center", gap: 18, padding: "10px 18px" }}>
        <div style={{ display: "flex", alignItems: "center", gap: 8 }}>
          <span style={{ fontFamily: "var(--f-title)", fontSize: 12, letterSpacing: "0.1em", color: "var(--ember-gold)" }}>语言</span>
          <span style={{ fontSize: 13, color: "var(--ember-gold)", borderBottom: "1px solid var(--ember-gold-dim)" }}>{lang}</span>
        </div>
        <span style={{ width: 1, height: 18, background: "var(--iron-edge)" }}/>
        <div style={{ display: "flex", gap: 16, flex: 1, flexWrap: "wrap" }}>
          {hints.map((h, i) => (
            <div key={i} style={{ display: "flex", alignItems: "center", gap: 5 }}>
              <div style={{ display: "flex", gap: 2 }}>{h.keys.map((k, j) => <KeyCap key={j} tone={h.tone}>{k}</KeyCap>)}</div>
              <span style={{ fontSize: 12, color: "var(--muted-text)" }}>{h.label}</span>
            </div>
          ))}
        </div>
        <div style={{ display: "flex", gap: 8 }}>
          {icons.map(ic => (
            <div key={ic.token} title={ic.title} style={{
              width: 34, height: 34, borderRadius: "50%",
              border: `1px solid ${ic.tone === "gold" ? "var(--ember-gold)" : "var(--iron-edge)"}`,
              background: "radial-gradient(60% 60% at 50% 35%, rgba(255,235,200,0.04), transparent), #0a0d13",
              display: "flex", alignItems: "center", justifyContent: "center",
              boxShadow: "inset 0 1px 0 rgba(255,255,255,0.04), inset 0 -2px 4px rgba(0,0,0,0.6)",
            }}>
              <IconToken token={ic.token} size={26} tone={ic.tone === "gold" ? "gold" : "iron"} shape="circle" fill="hollow" showLabel={false} />
            </div>
          ))}
        </div>
      </div>
    </MMFrame>
  );
}

window.MainMenu = MainMenu;
window.MM_CLASSES = MM_CLASSES;
window.MM_RACES = MM_RACES;
window.MM_MENU = MM_MENU;
