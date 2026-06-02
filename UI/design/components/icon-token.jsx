// components/icon-token.jsx
// Pure text-token placeholders for icons. No SVG. No drawn glyphs.
// Renders a labeled box that the renderer can later swap for sheet cells via manifest key.

function IconToken({
  token = "icon.unknown",
  size = 48,
  tone = "iron",            // iron | gold | cyan | red | green | violet | locked
  shape = "square",         // square | circle (rings/amulets)
  showLabel = true,
  hint = null,              // optional small annotation under the token name
  fill = "stone",           // stone | hollow
}) {
  const toneMap = {
    iron:   { border: "var(--iron-edge)",     text: "var(--muted-text)" },
    gold:   { border: "var(--ember-gold-dim)", text: "var(--ember-gold)" },
    cyan:   { border: "var(--cold-cyan-dim)",  text: "var(--cold-cyan)" },
    red:    { border: "var(--blood-red-dim)",  text: "var(--blood-red)" },
    green:  { border: "var(--stamina-green-dim)", text: "var(--stamina-green)" },
    violet: { border: "var(--arcane-violet-dim)", text: "var(--arcane-violet)" },
    locked: { border: "var(--talent-locked)",  text: "var(--talent-locked)" },
  };
  const t = toneMap[tone] || toneMap.iron;

  // Token text scales with box
  const tokenFs = size >= 64 ? 9 : size >= 48 ? 8 : 7;
  const hintFs  = size >= 64 ? 8 : 7;

  const inner = (
    <div style={{
      position: "absolute",
      inset: 0,
      display: "flex",
      flexDirection: "column",
      alignItems: "center",
      justifyContent: "center",
      gap: 1,
      padding: 2,
      textAlign: "center",
      fontFamily: "var(--f-mono)",
      fontSize: tokenFs,
      lineHeight: 1.15,
      color: t.text,
      letterSpacing: 0,
      wordBreak: "break-all",
    }}>
      {showLabel && (
        <>
          <span style={{ opacity: 0.55, fontSize: tokenFs - 1 }}>[icon]</span>
          <span style={{ opacity: 0.95 }}>{token}</span>
          {hint && <span style={{ opacity: 0.45, fontSize: hintFs }}>{hint}</span>}
        </>
      )}
    </div>
  );

  return (
    <div style={{
      width: size,
      height: size,
      position: "relative",
      borderRadius: shape === "circle" ? "50%" : "var(--r-md)",
      background: fill === "stone"
        ? "radial-gradient(60% 60% at 50% 40%, rgba(255,235,200,0.012), transparent 80%), #0A0E14"
        : "transparent",
      border: `1px solid ${t.border}`,
      boxShadow: fill === "stone"
        ? "inset 0 1px 0 rgba(255,255,255,0.025), inset 0 -1px 0 rgba(0,0,0,0.55)"
        : "none",
      overflow: "hidden",
      flex: "0 0 auto",
    }}>
      {/* diagonal stripe wash to read as "placeholder" */}
      <div style={{
        position: "absolute", inset: 0, pointerEvents: "none",
        background:
          "repeating-linear-gradient(135deg, rgba(255,255,255,0.018) 0 2px, transparent 2px 6px)",
        borderRadius: "inherit",
      }}/>
      {inner}
    </div>
  );
}

window.IconToken = IconToken;
